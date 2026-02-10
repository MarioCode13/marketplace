package dev.marketplace.marketplace.tools;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

public class PayFastSignatureTool {
    public static void main(String[] args) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("merchant_id", "12247699");
        params.put("merchant_key", "fdsyodv8kudog");
        params.put("amount", "100.00");
        params.put("item_name", "Pro Store Subscription");
        params.put("subscription_type", "1");
        params.put("recurring_amount", "100.00");
        params.put("frequency", "3");
        params.put("cycles", "0");
        params.put("custom_str1", "pro_store");
        params.put("custom_str2", "jimi%40gmail.com"); // encoded email like the user's include variants

        String passphrase = "d3ali0Mark3t";

        System.out.println("-- Generating variants for params (custom_str2 encoded):");
        Map<String, Object> v1 = buildVariant(params, true, true, passphrase);
        Map<String, Object> v2 = buildVariant(params, true, false, passphrase);
        Map<String, Object> v3 = buildVariant(params, false, true, passphrase);
        Map<String, Object> v4 = buildVariant(params, false, false, passphrase);

        System.out.println("include_encoded:\n" + v1);
        System.out.println("include_plain:\n" + v2);
        System.out.println("exclude_encoded:\n" + v3);
        System.out.println("exclude_plain:\n" + v4);
    }

    private static Map<String, Object> buildVariant(Map<String, String> params, boolean includeMerchantKey, boolean urlEncodeValues, String passphrase) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, String> filtered = new java.util.LinkedHashMap<>();
        params.forEach((k, v) -> { if (v != null && !v.isEmpty() && !"signature".equals(k)) filtered.put(k, v); });
        if (!includeMerchantKey) filtered.remove("merchant_key");

        StringBuilder sb = new StringBuilder();
        filtered.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            String toAppend = urlEncodeValues ? rfc3986Encode(e.getValue()) : e.getValue();
            sb.append(e.getKey()).append("=").append(toAppend).append("&");
        });
        if (sb.length() > 0 && sb.charAt(sb.length()-1) == '&') sb.setLength(sb.length()-1);

        if (passphrase != null && !passphrase.isBlank()) {
            sb.append("&passphrase=").append(passphrase);
        }

        String baseString = sb.toString();
        String md5 = md5Hex(baseString);
        String sha256 = sha256Hex(passphrase);

        result.put("baseString", baseString);
        result.put("signature", md5);
        result.put("passphrase_masked", mask(passphrase));
        result.put("passphrase_sha256", sha256);
        result.put("passphrase_length", passphrase.length());
        return result;
    }

    private static String rfc3986Encode(String s) {
        if (s == null) return "";
        String encoded = URLEncoder.encode(s, StandardCharsets.UTF_8);
        encoded = encoded.replace("+", "%20").replace("%7E", "~");
        return encoded;
    }

    private static String md5Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] b = md.digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private static String sha256Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] b = md.digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private static String mask(String p) {
        if (p == null) return "";
        if (p.length() <= 4) return "****";
        if (p.length() <= 8) return p.charAt(0) + "****" + p.charAt(p.length()-1);
        return p.substring(0,2) + "****" + p.substring(p.length()-2);
    }
}

