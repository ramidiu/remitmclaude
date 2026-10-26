package com.remitm.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final String OTP_PREFIX = "otp:";
    private static final int DEFAULT_OTP_TTL_MINUTES = 10;
    private static final Random RANDOM = new Random();

    private final StringRedisTemplate stringRedisTemplate;
    // System Controls: OTP lifetime is driven at runtime by the superadmin (system_config key
    // OTP_TTL_MINUTES), falling back to the 10-minute default when the config row is absent.
    private final com.remitm.modules.user.service.SystemConfigService systemConfigService;

    public String generateOtp() {
        int otp = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }

    public void storeOtp(String email, String otp) {
        String key = OTP_PREFIX + email;
        int ttlMinutes = systemConfigService.getInt("OTP_TTL_MINUTES", DEFAULT_OTP_TTL_MINUTES);
        stringRedisTemplate.opsForValue().set(key, otp, ttlMinutes, TimeUnit.MINUTES);
        log.debug("OTP stored for email: {} (ttl={}m)", email, ttlMinutes);
    }

    public boolean validateOtp(String email, String otp) {
        String key = OTP_PREFIX + email;
        String storedOtp = stringRedisTemplate.opsForValue().get(key);
        return otp != null && otp.equals(storedOtp);
    }

    public void deleteOtp(String email) {
        String key = OTP_PREFIX + email;
        stringRedisTemplate.delete(key);
        log.debug("OTP deleted for email: {}", email);
    }
}
