package myjdk.lock.spring;

@Component
public class PostProcessorImpl implements PostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean) {
        System.out.println("初始化之前"+bean);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean) {
        System.out.println("初始化之后"+bean);
        return bean;
    }
}
