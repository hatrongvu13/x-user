package com.htv.xuser.services.msg;

public interface MessageService {
    String get(String key, Object... args);
    String getOrDefault(String key, String defaultValue);
}
