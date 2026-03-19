package com.htv.xuser.services.msg;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/**
 * MessageService — resolve i18n message từ messages.properties
 *
 * Cách dùng:
 * <pre>
 *   msg.get("error.user.not.found")
 *   msg.get("error.user.account.locked", "15 phút")
 *   msg.getOrDefault("field.email", "email")
 * </pre>
 */

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageSource messageSource;

    @Override
    public String get(String key, Object... args) {
        return "";
    }

    @Override
    public String getOrDefault(String key, String defaultValue) {
        return "";
    }
}
