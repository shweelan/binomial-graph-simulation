package bn;

import bn.Message;

@FunctionalInterface
public interface MessageRouter {
    void route(Message message);
}
