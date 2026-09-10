package com.mindlab.worky.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Perfil minimo no formato que o backend /carreira/match espera
 * (mesmo shape do perfil web: form + skills).
 */
public final class MatchProfileBuilder {

    private MatchProfileBuilder() {}

    public static Map<String, Object> build(String bio, String skillsCsv) {
        Map<String, Object> form = new HashMap<>();
        form.put("bio", bio != null ? bio.trim() : "");
        form.put("nome", "");
        form.put("cidade", "");
        form.put("estado", "");

        List<Map<String, Object>> skills = new ArrayList<>();
        for (String token : splitSkills(skillsCsv)) {
            Map<String, Object> skill = new HashMap<>();
            skill.put("label", token);
            skill.put("type", "tech");
            skills.add(skill);
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("form", form);
        profile.put("skills", skills);
        profile.put("experiences", new ArrayList<>());
        profile.put("certs", new ArrayList<>());
        profile.put("educations", new ArrayList<>());
        return profile;
    }

    public static List<String> splitSkills(String skillsCsv) {
        List<String> out = new ArrayList<>();
        if (skillsCsv == null || skillsCsv.trim().isEmpty()) return out;
        String normalized = skillsCsv.replace('\n', ',').replace('·', ',');
        for (String part : normalized.split(",")) {
            String token = part.trim();
            if (!token.isEmpty() && !out.contains(token)) {
                out.add(token);
            }
        }
        return out;
    }

    public static String cacheKey(String carreira, String bio, String skillsCsv) {
        String c = carreira == null ? "" : carreira.trim().toLowerCase(Locale.ROOT);
        String b = bio == null ? "" : bio.trim().toLowerCase(Locale.ROOT);
        String s = String.join("|", splitSkills(skillsCsv)).toLowerCase(Locale.ROOT);
        return c + "#" + b.hashCode() + "#" + s;
    }
}
