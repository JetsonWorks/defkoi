package com.jit.defkoi;

import lombok.NoArgsConstructor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@NoArgsConstructor
public class ThreadGroupLocal<T> extends ThreadLocal<ThreadGroupLocal.ValueHolder> {
  static class ValueHolder {
    public Object value;
  }

  // Weak & Concurrent would be even the better, but Java API won't offer that :(
  private static ConcurrentMap<ThreadGroup, ValueHolder> map = new ConcurrentHashMap<>();

  private static ValueHolder valueHolderForThread(Thread t) {
    map.putIfAbsent(t.getThreadGroup(), new ValueHolder());
    return map.get(t.getThreadGroup());
  }

  @Override
  protected ValueHolder initialValue() {
    return valueHolderForThread(Thread.currentThread());
  }

  public ThreadGroupLocal(T value) {
    setValue(value);
  }

  public T getValue() {
    return (T)get().value;
  }

  public void setValue(T value) {
    get().value = value;
  }

}
