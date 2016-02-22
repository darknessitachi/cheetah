package cheetah.distributor.core.support;

import cheetah.distributor.core.DispatcherWorker;
import cheetah.distributor.core.EventMessage;
import cheetah.distributor.core.EventResult;
import cheetah.distributor.event.AbstractCollector;
import cheetah.distributor.event.Event;

/**
 * Created by Max on 2016/2/3.
 */
public class GenericEventCollector extends AbstractCollector {
    public GenericEventCollector() {
    }

    public GenericEventCollector(DispatcherWorker dispatcher) {
        super(dispatcher);
    }

    @Override
    public void collect(Event event) {
        getDispatcher().receive(new EventMessage(event));
    }

    @Override
    public void collect(Event event, boolean fisrtWin) {
        getDispatcher().receive(new EventMessage(event, fisrtWin));
    }

    @Override
    public EventResult collect(boolean needResult, Event event) {
        return getDispatcher().receive(new EventMessage(needResult, event));
    }

    @Override
    public EventResult collect(boolean needResult, boolean fisrtWin, Event event) {
        return getDispatcher().receive(new EventMessage(event, needResult, fisrtWin));
    }
}