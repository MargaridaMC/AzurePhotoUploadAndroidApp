package com.example.myapplication;

import java.util.ArrayList;

public interface OutboxCheck {
    void checkOutbox(ArrayList<String> filenames, boolean fileIsThere);
}
