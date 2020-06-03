package project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class CalendarDto {

    private List<Integer> years;

    private Map<String, Integer> posts;
}
