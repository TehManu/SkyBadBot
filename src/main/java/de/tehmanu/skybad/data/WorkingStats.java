package de.tehmanu.skybad.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * @author TehManu
 * @since 20.10.2024
 */
@Getter
@AllArgsConstructor
public class WorkingStats {

    private final LocalDate date;
    private final LocalTime today;
    private final int workingTime;
    private final int totalWorkingTime;
}
