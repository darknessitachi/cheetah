package org.cheetah.fighter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.lmax.disruptor.dsl.Disruptor;
import org.cheetah.common.Startable;
import org.cheetah.common.logger.Info;
import org.cheetah.common.logger.Warn;
import org.cheetah.common.utils.CollectionUtils;
import org.cheetah.common.utils.ObjectUtils;
import org.cheetah.common.utils.StringUtils;
import org.cheetah.fighter.engine.Engine;
import org.cheetah.fighter.engine.EngineDirector;
import org.cheetah.fighter.engine.support.EngineStrategy;
import org.cheetah.fighter.handler.Handler;
import org.cheetah.fighter.plugin.Plugin;
import org.cheetah.fighter.plugin.PluginChain;
import org.cheetah.fighter.worker.Worker;
import org.cheetah.fighter.worker.WorkerAdapter;
import org.cheetah.fighter.worker.support.DisruptorWorkerAdapter;
import org.cheetah.fighter.worker.support.ForeseeableWorkerAdapter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Created by Max on 2016/1/29.
 */
public class EventBus extends Dispatcher implements Startable {

    private FighterConfig fighterConfig; //框架配置
    private List<Plugin> plugins = ImmutableList.of();
    private List<Interceptor> interceptors = ImmutableList.of();
    private List<DomainEventListener> eventListeners = ImmutableList.of();
    private final PluginChain pluginChain = new PluginChain();
    private Engine engine;
    private EngineDirector engineDirector;
    private EngineStrategy engineStrategy;
    private final Map<InterceptorCacheKey, List<Interceptor>> interceptorCache;
    private Map<HandlerMapperKey, List<Handler>> eventHandlers;
    private final ReentrantLock lock;
    private final EventContext context;

    public EventBus() {
        this.interceptorCache = new ConcurrentHashMap<>();
        this.eventHandlers = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
        this.context = EventContext.getContext();
    }

    /**
     * 接收事件消息，安排相应的engine对其进行处理
     *
     * @param eventMessage
     * @return
     */
    @Override
    public EventResult receive(final EventMessage eventMessage) {
        try {
            context().setEventMessage(eventMessage);
            DomainEvent event = eventMessage.event();
            HandlerMapperKey key = HandlerMapperKey.generate(event.getClass(), event.getSource().getClass());
            List<Interceptor> interceptors = findInterceptor(event);
            context.setInterceptor(interceptors);
            boolean exists = eventHandlers.containsKey(key);
            if (exists) {
                List<Handler> handlerMap = eventHandlers.get(key);
                context.setHandlers(handlerMap);
                return dispatch();
            }
            List<Handler> handlers = getHandlers(event, key);
            if (handlers.isEmpty()) {
                Warn.log(this.getClass(), "Couldn't find the corresponding mapping.");
                throw new NoMapperException();
            }
            context.setHandlers(handlers);
            return dispatch();
        } finally {
            context.removeEventMessage();
            context.removeHandlers();
            context.removeInterceptor();
        }
    }

    @Override
    public EventResult dispatch() {
        EventMessage eventMessage = context().getEventMessage();
        return doDispatch(eventMessage);
    }

    private EventResult doDispatch(EventMessage eventMessage) {
        WorkerAdapter workerAdapter = getWorkerAdapter(this.engineStrategy);
        if(workerAdapter instanceof DisruptorWorkerAdapter)
            ((DisruptorWorkerAdapter) workerAdapter).setRingBuffer(((Disruptor) engine.getAsynchronous()).getRingBuffer());
        if(workerAdapter instanceof ForeseeableWorkerAdapter)
            ((ForeseeableWorkerAdapter) workerAdapter).setWorkers((Worker[]) engine.getAsynchronous());
        Feedback feedback = workerAdapter.work(eventMessage);
        return new EventResult(eventMessage.event(), feedback.isSuccess());
    }

    @Override
    public void start() {
        if (fighterConfig != null) {
            if (!plugins.isEmpty())
                initializesPlugin(pluginChain);
        } else
            fighterConfig = new FighterConfig();
        if (StringUtils.isEmpty(fighterConfig.getEngine())) {
            engineStrategy = EngineStrategy.DISRUPTOR;
            engineDirector = engineStrategy.getEngineDirector();
        } else {
            engineStrategy = EngineStrategy.getEngine(fighterConfig.getEngine());
            engineDirector = engineStrategy.getEngineDirector();
        }
        engineDirector.setFighterConfig(this.fighterConfig);
        engine = engineDirector.directEngine();
        engine.setContext(this.context());
        engine.registerPluginChain(pluginChain);
        engine.start();
        Info.log(this.getClass(), "EventBus start engine is {}", engineStrategy.name());
    }

    @Override
    public void stop() {
        this.engine.stop();
        this.engine = null;
        engineDirector = null;
        engineStrategy = null;
        Warn.log(this.getClass(), "EventBus stop...");
    }

    private List<Handler> getHandlers(final DomainEvent event, final HandlerMapperKey mapperKey) {
        return resolve(event, mapperKey);
    }

    /**
     * Mapper helper
     *
     * @param event
     */
    private List<Handler> resolve(final DomainEvent event, final HandlerMapperKey mapperKey) {
        lock.lock();
        try {
            if (DomainEvent.class.isAssignableFrom(event.getClass())) {
                List<DomainEventListener> eventListeners = supportsSmartListener(event);
                if (eventListeners.isEmpty()) {
                    eventListeners = supportsUniversalListener(event);
                }
                return assembleEventHandlerMapping(mapperKey, eventListeners);
            }
        } finally {
            lock.unlock();
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<DomainEventListener> supportsUniversalListener(final Event event) {
        List<DomainEventListener> eventListeners = this.eventListeners;

        return eventListeners.stream().filter(o -> {
            List classes = CollectionUtils.arrayToList(o.getClass().getInterfaces());
            Optional<Class> classOptional = classes.stream().filter(it -> it.equals(DomainEventListener.class)).findFirst();
            if (!classOptional.isPresent())
                return false;
            Type type = o.getClass().getGenericInterfaces()[0];
            if (!(type instanceof ParameterizedType))
                return true;
            Type[] parameterizedType = ((ParameterizedType) type).getActualTypeArguments();
            if (parameterizedType.length < 1)
                return true;
            return event.getClass().equals(parameterizedType[0]);
        }).collect(Collectors.toList());
    }

    private List<Handler> assembleEventHandlerMapping(HandlerMapperKey mapperKey,
                                                      List<DomainEventListener> listeners) {
        List<Handler> handlers = Lists.newArrayList();
        for (DomainEventListener listener : listeners) {
            Handler handler = engine.assignDomainEventHandler(listener);
            handlers.add(handler);
        }

        eventHandlers.put(mapperKey, handlers);
        return handlers;
    }

    /**
     * 根据domainevent过滤出相应的listener
     * @param event
     * @return
     */
    private List<DomainEventListener> supportsSmartListener(final DomainEvent event) {
        List<DomainEventListener> list = this.eventListeners;

        return list.stream().filter(o -> SmartDomainEventListener.class.isAssignableFrom(o.getClass()))
                .map(o -> ((SmartDomainEventListener) o))
                .filter(o -> o.supportsEventType(event.getClass()))
                .filter(o -> o.supportsSourceType(event.getSource().getClass()))
                .collect(Collectors.toList());
    }
    private PluginChain initializesPlugin(PluginChain chain) {
        Objects.requireNonNull(chain, "chain must not be null");
        for (Plugin plugin : this.plugins)
            chain.register(plugin);
        return chain;
    }

    private List<Interceptor> findInterceptor(final DomainEvent event) {
        List<Interceptor> interceptors = Lists.newArrayList(this.interceptors);
        if (CollectionUtils.isEmpty(interceptors))
            return Collections.emptyList();
        InterceptorCacheKey key = new InterceptorCacheKey(event.getClass());
        List<Interceptor> $interceptors = interceptorCache.get(key);
        if (CollectionUtils.isEmpty($interceptors)) {
            $interceptors = interceptors.stream().filter(o -> o.supportsType(event.getClass())).collect(Collectors.toList());
            interceptorCache.put(key, $interceptors);
        }
        return $interceptors;
    }

    private List<WorkerAdapter> getDefaultWorkerAdapter() {
        return Lists.newArrayList(
                new DisruptorWorkerAdapter(),
                new ForeseeableWorkerAdapter()
        );
    }

    private WorkerAdapter getWorkerAdapter(EngineStrategy engineStrategy) {
        return getDefaultWorkerAdapter()
                .stream()
                .filter(o -> o.supports(engineStrategy))
                .findFirst()
                .get();
    }

    public void registerPlugins(List<Plugin> plugins) {
        this.plugins = ImmutableList.<Plugin>builder().addAll(this.plugins).addAll(plugins).build();
    }

    public void registerInterceptors(List<Interceptor> interceptors) {
        this.interceptors = ImmutableList.<Interceptor>builder().addAll(this.interceptors).addAll(interceptors).build();
    }

    public void registerEventListeners(List<DomainEventListener> eventListeners) {
        this.eventListeners = ImmutableList.<DomainEventListener>builder().addAll(this.eventListeners).addAll(eventListeners).build();
    }

    /**
     * getter and setter
     */
    public void setFighterConfig(FighterConfig fighterConfig) {
        this.fighterConfig = fighterConfig;
    }

    protected FighterConfig fighterConfig() {
        return fighterConfig;
    }

    protected PluginChain pluginChain() {
        return pluginChain;
    }

    protected EventContext context() {
        return context;
    }

    protected Engine engine() {
        return engine;
    }

    protected EngineDirector getEngineDirector() {
        return engineDirector;
    }

    protected EngineStrategy getEngineStrategy() {
        return engineStrategy;
    }

    public void setPlugins(List<Plugin> plugins) {
        this.plugins = ImmutableList.<Plugin>builder().addAll(this.plugins).addAll(plugins).build();
    }

    public void setInterceptors(List<Interceptor> interceptors) {
        this.interceptors = ImmutableList.<Interceptor>builder().addAll(this.interceptors).addAll(interceptors).build();
    }

    public void setEventListeners(List<DomainEventListener> eventListeners) {
        this.eventListeners = ImmutableList.<DomainEventListener>builder().addAll(this.eventListeners).addAll(eventListeners).build();
    }

    static class InterceptorCacheKey {
        private Class<? extends Event> eventClz;

        public InterceptorCacheKey(Class<? extends Event> eventClz) {
            this.eventClz = eventClz;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            InterceptorCacheKey that = (InterceptorCacheKey) o;

            return ObjectUtils.nullSafeEquals(this.eventClz, that.eventClz);
        }

        @Override
        public int hashCode() {
            return ObjectUtils.nullSafeHashCode(this.eventClz) * 29;
        }
    }

    public static class HandlerMapperKey {
        private final Class<?> eventType;
        private final Class<?> sourceType;

        public HandlerMapperKey(Class<?> eventType, Class<?> sourceType) {
            this.eventType = eventType;
            this.sourceType = sourceType;
        }

        public static HandlerMapperKey generate(Class<?> eventType, Class<?> sourceType) {
            return new HandlerMapperKey(eventType, sourceType);
        }

        public static HandlerMapperKey generate(Event event) {
            return new HandlerMapperKey(event.getClass(), event.getSource().getClass());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            HandlerMapperKey that = (HandlerMapperKey) o;

            return ObjectUtils.nullSafeEquals(this.eventType, that.eventType) && ObjectUtils.nullSafeEquals(this.sourceType, that.sourceType);
        }

        @Override
        public int hashCode() {
            return ObjectUtils.nullSafeHashCode(this.eventType) * 29 + ObjectUtils.nullSafeHashCode(this.sourceType);
        }
    }
}