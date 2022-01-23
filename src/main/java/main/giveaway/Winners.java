package main.giveaway;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Winners {

    private Integer n;
    private Integer min;
    private Integer max;

    @Override
    public String toString() {
        return "{" +
                "\"n\": \"" + n + "\"" +
                ", \"min\": \"" + min + "\"" +
                ", \"max\": " + max + '}';
    }
}