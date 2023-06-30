package com.anderhurtado.spigot.mobmoney.util;

import com.anderhurtado.spigot.mobmoney.util.function.Decode;
import com.anderhurtado.spigot.mobmoney.util.function.Max;
import com.anderhurtado.spigot.mobmoney.util.function.Min;
import com.anderhurtado.spigot.mobmoney.util.function.Random;
import net.objecthunter.exp4j.ExpressionBuilder;

public class PreDefinedExpression extends ExpressionBuilder {

    public PreDefinedExpression(String expression) {
        super(expression);
        functions(Max.getInstance(), Min.getInstance(), Decode.getInstance(), Random.getInstance());
    }

}
