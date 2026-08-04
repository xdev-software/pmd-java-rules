/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package software.xdev.pmd.rule.prettyprint;

import static net.sourceforge.pmd.util.OptionalBool.UNKNOWN;

import org.pcollections.PSet;

import net.sourceforge.pmd.lang.java.symbols.SymbolicValue;
import net.sourceforge.pmd.util.OptionalBool;


/**
 * Options to pretty print a type. Cannot be used concurrently.
 */
@SuppressWarnings("all") // Upstream code from https://github.com/pmd/pmd/pull/6904
public class TypePrettyPrinter
{
	protected final StringBuilder sb = new StringBuilder();
	
	protected boolean printMethodHeader = true;
	protected boolean printMethodReturnType = true;
	protected OptionalBool printTypeVarBounds = UNKNOWN;
	protected boolean qualifyTvars = false;
	protected boolean qualifyNames = true;
	protected boolean isVarargs = false;
	protected boolean printTypeAnnotations = true;
	protected boolean qualifyAnnotations = false;
	
	/**
	 * Create a new pretty printer with the default configuration.
	 */
	public TypePrettyPrinter()
	{
		// default
	}
	
	public StringBuilder append(final char o)
	{
		return this.sb.append(o);
	}
	
	public StringBuilder append(final String o)
	{
		return this.sb.append(o);
	}
	
	/**
	 * Print the declaring type of the method and its type parameters. Default: true.
	 */
	public TypePrettyPrinter printMethodHeader(final boolean printMethodHeader)
	{
		this.printMethodHeader = printMethodHeader;
		return this;
	}
	
	/**
	 * Print the return type of methods (as postfix). Default: true.
	 */
	public TypePrettyPrinter printMethodResult(final boolean printMethodResult)
	{
		this.printMethodReturnType = printMethodResult;
		return this;
	}
	
	/**
	 * Print the bounds of type variables. Default: false.
	 */
	public void printTypeVarBounds(final OptionalBool printTypeVarBounds)
	{
		this.printTypeVarBounds = printTypeVarBounds;
	}
	
	/**
	 * Qualify type variables with the name of the declaring symbol. Eg {@code Foo#T} for {@code class Foo<T>}}.
	 * Default: false.
	 */
	public TypePrettyPrinter qualifyTvars(final boolean qualifyTvars)
	{
		this.qualifyTvars = qualifyTvars;
		return this;
	}
	
	/**
	 * Whether to print the binary name of a type annotation or just the simple name if false. Default: false.
	 */
	public TypePrettyPrinter qualifyAnnotations(final boolean qualifyAnnotations)
	{
		this.qualifyAnnotations = qualifyAnnotations;
		return this;
	}
	
	/**
	 * Whether to print the type annotations. Default: true.
	 */
	public TypePrettyPrinter printAnnotations(final boolean printAnnotations)
	{
		this.printTypeAnnotations = printAnnotations;
		return this;
	}
	
	/**
	 * Use qualified names for class types instead of simple names. Default: true.
	 */
	public TypePrettyPrinter qualifyNames(final boolean qualifyNames)
	{
		this.qualifyNames = qualifyNames;
		return this;
	}
	
	public String consumeResult()
	{
		// The pretty printer might be reused by another call,
		// delete the buffer.
		final String result = this.sb.toString();
		this.sb.setLength(0);
		return result;
	}
	
	protected void printTypeAnnotations(final PSet<SymbolicValue.SymAnnot> annots)
	{
		if(this.printTypeAnnotations)
		{
			for(final SymbolicValue.SymAnnot annot : annots)
			{
				final String name = this.qualifyAnnotations ? annot.getBinaryName()
					: annot.getSimpleName();
				this.append('@').append(name).append(' ');
			}
		}
	}
}
