package org.cheetah.fighter.governor;

import org.cheetah.fighter.EventMessage;
import org.cheetah.fighter.Feedback;
import org.cheetah.fighter.handler.Handler;

import java.util.List;

/**
 * 事件管理者接口
 * Created by Max on 2016/2/20.
 */
@Deprecated
public interface Governor extends Cloneable {

    Governor reset();

    /**
     * 重置内部管理的数据
     * @return
     */
    Governor initialize();

    /**
     * 管理者下达一个命令让工人开始工作
     * @return
     */
    Feedback command();

    /**
     * 承担当前消息的任务
     * @param eventMessage
     * @return
     */
    Governor accept(EventMessage eventMessage);

    EventMessage details();

    /**
     * 获取管理者唯一的标示符
     * @return
     */
    String getId();

    /**
     * 注册工人需要处理器
     * @param handlers
     * @return
     */
    Governor registerHandlerSquad(List<Handler> handlers);

    /**
     * 开除一个工作处理器
     * @param handler
     */
    void unRegisterHandler(Handler handler);

    /**
     * 分身术-即拷贝一个管理者
     * @return
     * @throws CloneNotSupportedException
     */
    Governor kagebunsin() throws CloneNotSupportedException;

}