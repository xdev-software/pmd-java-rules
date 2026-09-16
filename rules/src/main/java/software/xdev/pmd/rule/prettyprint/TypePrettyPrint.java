/*
 * Copyright © 2026 XDEV Software (https://xdev.software)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package software.xdev.pmd.rule.prettyprint;

import org.checkerframework.checker.nullness.qual.NonNull;

import net.sourceforge.pmd.lang.java.types.JTypeVisitable;


/**
 * Pretty-printing methods to display types. The current API is only offered for debugging, not for displaying types to
 * users.
 */
@SuppressWarnings("all") // Upstream code from https://github.com/pmd/pmd/pull/6904
public final class TypePrettyPrint
{
	
	private TypePrettyPrint()
	{
	}
	
	public static @NonNull String prettyPrint(@NonNull final JTypeVisitable t)
	{
		return prettyPrint(t, new TypePrettyPrinter());
	}
	
	public static @NonNull String prettyPrintWithSimpleNames(@NonNull final JTypeVisitable t)
	{
		return prettyPrint(t, new TypePrettyPrinter().qualifyNames(false));
	}
	
	public static String prettyPrint(@NonNull final JTypeVisitable t, final TypePrettyPrinter prettyPrinter)
	{
		t.acceptVisitor(DefaultVisitor.INSTANCE, prettyPrinter);
		return prettyPrinter.consumeResult();
	}
	
	static final class DefaultVisitor extends PrettyPrintVisitor<TypePrettyPrinter>
	{
		static final DefaultVisitor INSTANCE = new DefaultVisitor();
	}
}
