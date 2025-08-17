package myjdk.lock;

import java.util.Map;

public interface Lock {
    public boolean lock();
    public boolean unlock();

    Map getMap();

    Lock getLockImpl();
}
