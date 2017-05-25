package com.bmc.truesight.remedy.util;

import java.text.MessageFormat;

public class StringUtils {

	public static String format(String template, Object[] args){
		MessageFormat fmt = new MessageFormat(template);
		return fmt.format(args);
	}
}
