package com.mindlab.worky.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ProfileMatchResult {

    @SerializedName("pct")
    public int pct;

    @SerializedName("matched")
    public List<String> matched;

    @SerializedName("gaps")
    public List<String> gaps;

    @SerializedName("explanation")
    public String explanation;

    public List<String> matchedSafe() {
        return matched != null ? matched : new ArrayList<>();
    }

    public List<String> gapsSafe() {
        return gaps != null ? gaps : new ArrayList<>();
    }
}
