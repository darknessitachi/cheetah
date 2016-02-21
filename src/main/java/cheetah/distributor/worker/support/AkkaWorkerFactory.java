package cheetah.distributor.worker.support;

import cheetah.distributor.worker.Worker;
import cheetah.distributor.worker.WorkerFactory;

/**
 * Created by Max on 2016/2/21.
 */
public class AkkaWorkerFactory implements WorkerFactory {

    @Override
    public Worker createApplicationEventWorker() {
        return new ApplicationEventWorker();
    }

    @Override
    public Worker createDomainEventWorker() {
        return new DomainEventWorker();
    }

}
