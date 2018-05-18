package bn;

import bn.Message;

@FunctionalInterface
public interface MessageRouter {
  boolean route(Message message);
}
