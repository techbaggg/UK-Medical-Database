package com.drgenz;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class MedOrderTest {

    @Test
    public void tabletsAndCapsulesRankZero() {
        assertEquals(0, MedOrder.formRank("Paracetamol 500mg Tablets"));
        assertEquals(0, MedOrder.formRank("Amoxicillin 250mg capsules"));
        assertEquals(0, MedOrder.formRank("Ibuprofen caplet"));
        assertEquals(0, MedOrder.formRank("Aspirin 75mg tab"));
        assertEquals(0, MedOrder.formRank("Omeprazole 20mg cap"));
    }

    @Test
    public void otherFormsRankOne() {
        assertEquals(1, MedOrder.formRank("Paracetamol Oral Suspension 120mg/5ml"));
        assertEquals(1, MedOrder.formRank("Morphine Sulfate 10mg/ml solution for injection"));
        assertEquals(1, MedOrder.formRank("Hydrocortisone 1% cream"));
        assertEquals(1, MedOrder.formRank("Salbutamol 100mcg inhaler"));
    }

    @Test
    public void tabletsFirstOrderingIsStable() {
        List<String> medicines = Arrays.asList(
                "Paracetamol 120mg/5ml syrup",
                "Paracetamol 500mg tablets",
                "Paracetamol 10mg/ml injection",
                "Paracetamol 500mg capsules"
        );

        List<String> ordered = MedOrder.tabletsFirst(medicines, m -> m);
        assertEquals(4, ordered.size());
        assertEquals("Paracetamol 500mg tablets", ordered.get(0));
        assertEquals("Paracetamol 500mg capsules", ordered.get(1));
        assertEquals("Paracetamol 120mg/5ml syrup", ordered.get(2));
        assertEquals("Paracetamol 10mg/ml injection", ordered.get(3));
    }
}
