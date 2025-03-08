package dev.marketplace.marketplace.resolvers;
import dev.marketplace.marketplace.enums.Condition;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.util.Arrays;
import java.util.List;

@Controller
public class ConditionQueryResolver {

    public ConditionQueryResolver() {
        System.out.println("ConditionQueryResolver initialized!");
    }

    @QueryMapping
    public List<Condition> getConditions() {
        System.out.println("Fetching Conditions...");
        return Arrays.asList(Condition.values());
    }
}
