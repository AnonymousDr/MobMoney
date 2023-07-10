package com.anderhurtado.spigot.mobmoney.util;

import com.anderhurtado.spigot.mobmoney.util.function.Decode;
import com.anderhurtado.spigot.mobmoney.util.function.Max;
import com.anderhurtado.spigot.mobmoney.util.function.Min;
import com.anderhurtado.spigot.mobmoney.util.function.Random;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreDefinedExpression extends ExpressionBuilder {

    private static final Pattern RANDOM_FIXER = Pattern.compile("random\\(\\d+\\)");

    private static String fix(String expression) {
        if(expression.contains("random")) {
            if(expression.contains("random()")) expression = expression.replace("random()", "random(0,1)");
            Matcher matcher;
            while((matcher = RANDOM_FIXER.matcher(expression)).find()) {
                String group = matcher.group();
                group = "random(0," + group.substring(7);
                expression = expression.replace(matcher.group(), group);
            }
        }
        return expression;
    }

    public PreDefinedExpression(String expression) {
        super(fix(expression));
        functions(Max.getInstance(), Min.getInstance(), Decode.getInstance(), Random.getInstance());
    }

}
