package com.babyorm;


import com.babyorm.util.Case;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CaseTest {

    @Test
    public void testAllConversion(){
        for(int i=0; i<Case.values().length; i++){
            for(int j=0; j<Case.values().length; j++){
                Case toCase = Case.values()[i];
                Case fromCase = Case.values()[j];
                String expected = toCase.whatDoesItLookLike();
                String converted = Case.convert(fromCase.whatDoesItLookLike(), toCase);
                System.out.println("Converted: " + fromCase.whatDoesItLookLike() + " to " + converted );
                if(fromCase.isWordPreserving()){
                    assertEquals(expected, converted,
                            "Failed to convert from: "+fromCase.name()+" to: "+toCase.name());
                } else {
                    System.out.println("non word preserving from case can't be converted to any case deterministically");
                }
            }
        }
    }


    @Test
    void convertSingleWord_kebab(){
        assertEquals("string", Case.convert("string", Case.KEBAB_CASE));
    }

    @Test
    void convertSingleWord_snake(){
        assertEquals("string", Case.convert("string", Case.SNAKE_CASE));
    }

    @Test
    void convertSingleWord_upperKebab(){
        assertEquals("STRING", Case.convert("string", Case.UPPER_KEBAB_CASE));
    }

    @Test
    void convertSingleWord_upperSnake(){
        assertEquals("STRING", Case.convert("string", Case.UPPER_SNAKE_CASE));
    }

    @Test
    void convertSingleWord_pascal(){
        assertEquals("String", Case.convert("string", Case.PASCAL_CASE));
    }
    @Test
    void convertSingleWord_camel(){
        assertEquals("string", Case.convert("string", Case.CAMEL_CASE));
    }

}
