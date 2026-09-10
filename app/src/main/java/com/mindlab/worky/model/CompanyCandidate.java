package com.mindlab.worky.model;

import java.util.ArrayList;
import java.util.List;

public class CompanyCandidate {

    public String id;
    public String name;
    public String role;
    public int match;
    public List<String> skills = new ArrayList<>();
    public String summary;
    public String email;
    public String location;
    public boolean locked;
}
