package com.resimulators.simukraft.common.enums;

public enum Category {

    RESIDENTIAL("residential", 1),
    INDUSTRIAL("industrial", 2),
    COMMERCIAL("commercial", 3),
    SPECIAL("special", 4);

    public String category;
    public int id;
    Category(String string,int id)
    {
        this.category = string;
        this.id = id;
    }

}
