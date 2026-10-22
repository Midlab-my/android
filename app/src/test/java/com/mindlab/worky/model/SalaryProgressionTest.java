package com.mindlab.worky.model;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SalaryProgressionTest {

    @Test
    public void emptyFallsBackToConsulta() {
        List<SalaryProgression.Band> bands = SalaryProgression.from("");
        assertEquals(3, bands.size());
        assertEquals("Pleno", bands.get(1).label);
        assertTrue(bands.get(1).active);
        assertEquals("Sob consulta", bands.get(1).range);
    }

    @Test
    public void singleValueBuildsThreeBands() {
        List<SalaryProgression.Band> bands = SalaryProgression.from("R$ 8 mil");
        assertEquals(3, bands.size());
        assertTrue(bands.get(0).range.contains("4.800") || bands.get(0).range.contains("4800"));
        assertTrue(bands.get(1).active);
    }

    @Test
    public void rangeUsesMinAndMax() {
        List<SalaryProgression.Band> bands = SalaryProgression.from("R$ 5.000 a R$ 9.000");
        assertEquals(3, bands.size());
        assertEquals("Júnior", bands.get(0).label);
        assertEquals("Sênior", bands.get(2).label);
    }
}
