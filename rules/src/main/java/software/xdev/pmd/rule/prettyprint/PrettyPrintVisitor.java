/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package software.xdev.pmd.rule.prettyprint;

import static net.sourceforge.pmd.util.OptionalBool.NO;
import static net.sourceforge.pmd.util.OptionalBool.YES;

import java.util.Arrays;
import java.util.List;

import net.sourceforge.pmd.lang.java.symbols.JTypeParameterSymbol;
import net.sourceforge.pmd.lang.java.types.JArrayType;
import net.sourceforge.pmd.lang.java.types.JClassType;
import net.sourceforge.pmd.lang.java.types.JIntersectionType;
import net.sourceforge.pmd.lang.java.types.JMethodSig;
import net.sourceforge.pmd.lang.java.types.JPrimitiveType;
import net.sourceforge.pmd.lang.java.types.JTypeMirror;
import net.sourceforge.pmd.lang.java.types.JTypeVar;
import net.sourceforge.pmd.lang.java.types.JTypeVisitor;
import net.sourceforge.pmd.lang.java.types.JWildcardType;
import net.sourceforge.pmd.lang.java.types.internal.infer.InferenceVar;
import net.sourceforge.pmd.util.OptionalBool;


@SuppressWarnings("all") // Upstream code from https://github.com/pmd/pmd/pull/6904
public class PrettyPrintVisitor<P extends TypePrettyPrinter> implements JTypeVisitor<Void, P>
{
	@Override
	public Void visit(final JTypeMirror t, final P sb)
	{
		sb.printTypeAnnotations(t.getTypeAnnotations());
		sb.append(t.toString());
		return null;
	}
	
	@Override
	public Void visitClass(final JClassType t, final P sb)
	{
		final JClassType enclosing = t.getEnclosingType();
		final boolean isAnon = t.getSymbol().isAnonymousClass();
		
		if(enclosing != null && !isAnon)
		{
			this.visitClass(enclosing, sb);
			sb.append('#');
		}
		else if(t.hasErasedSuperTypes() && !t.isRaw())
		{
			sb.append("(erased) ");
		}
		
		sb.printTypeAnnotations(t.getTypeAnnotations());
		
		if(t.getSymbol().isUnresolved())
		{
			sb.append('*'); // a small marker to spot them
		}
		
		this.appendClassName(t, sb, enclosing, isAnon);
		
		final List<JTypeMirror> targs = t.getTypeArgs();
		if(t.isRaw() || targs.isEmpty())
		{
			return null;
		}
		
		if(t.isGenericTypeDeclaration() && sb.printTypeVarBounds != NO)
		{
			sb.printTypeVarBounds = YES;
		}
		this.join(sb, targs, ", ", "<", ">");
		return null;
	}
	
	protected void appendClassName(final JClassType t, final P sb, final JClassType enclosing, final boolean isAnon)
	{
		if(enclosing != null && !isAnon || !sb.qualifyNames)
		{
			sb.append(t.getSymbol().getSimpleName());
		}
		else
		{
			sb.append(t.getSymbol().getBinaryName());
		}
	}
	
	@Override
	public Void visitWildcard(final JWildcardType t, final P sb)
	{
		sb.printTypeAnnotations(t.getTypeAnnotations());
		sb.append("?");
		if(t.isUnbounded())
		{
			return null;
		}
		
		sb.append(t.isUpperBound() ? " extends " : " super ");
		
		t.getBound().acceptVisitor(this, sb);
		return null;
	}
	
	@Override
	public Void visitPrimitive(final JPrimitiveType t, final P sb)
	{
		sb.printTypeAnnotations(t.getTypeAnnotations());
		sb.append(t.getSimpleName());
		return null;
	}
	
	@Override
	public Void visitTypeVar(final JTypeVar t, final P sb)
	{
		if(!t.isCaptured() && sb.qualifyTvars)
		{
			final JTypeParameterSymbol sym = t.getSymbol();
			if(sym != null)
			{
				sb.append(sym.getDeclaringSymbol().getSimpleName());
				sb.append('#');
			}
		}
		
		sb.printTypeAnnotations(t.getTypeAnnotations());
		sb.append(t.getName());
		
		if(sb.printTypeVarBounds == YES)
		{
			sb.printTypeVarBounds = NO;
			if(!t.getUpperBound().isTop())
			{
				sb.append(" extends ");
				t.getUpperBound().acceptVisitor(this, sb);
			}
			if(!t.getLowerBound().isBottom())
			{
				sb.append(" super ");
				t.getLowerBound().acceptVisitor(this, sb);
			}
			sb.printTypeVarBounds = YES;
		}
		return null;
	}
	
	/**
	 * Formats {@link Arrays#asList(Object[])} as {@code <T> asList(T...) -> List<T>}
	 */
	@Override
	public Void visitMethodType(final JMethodSig t, final P sb)
	{
		if(sb.printMethodHeader)
		{
			t.getDeclaringType().acceptVisitor(this, sb);
			sb.append(".");
			
			if(t.isGeneric())
			{
				final OptionalBool printBounds = sb.printTypeVarBounds;
				if(printBounds != NO)
				{
					sb.printTypeVarBounds = YES;
				}
				this.join(sb, t.getTypeParameters(), ", ", "<", "> ", false);
				sb.printTypeVarBounds = printBounds;
			}
		}
		
		sb.append(t.getName());
		
		this.join(sb, t.getFormalParameters(), ", ", "(", ")", t.isVarargs());
		
		if(sb.printMethodReturnType)
		{
			sb.append(" -> ");
			t.getReturnType().acceptVisitor(this, sb);
		}
		return null;
	}
	
	@Override
	public Void visitIntersection(final JIntersectionType t, final P sb)
	{
		return this.join(sb, t.getComponents(), " & ", "", "");
	}
	
	@Override
	public Void visitArray(final JArrayType t, final P sb)
	{
		final JTypeMirror component = t.getComponentType();
		if(component instanceof JIntersectionType)
		{
			sb.append("(");
		}
		
		final boolean isVarargs = sb.isVarargs;
		sb.isVarargs = false;
		component.acceptVisitor(this, sb);
		
		if(component instanceof JIntersectionType)
		{
			sb.append(")");
		}
		sb.printTypeAnnotations(t.getTypeAnnotations());
		sb.append(isVarargs ? "..." : "[]");
		return null;
	}
	
	@Override
	public Void visitNullType(final JTypeMirror t, final P sb)
	{
		sb.append("null");
		return null;
	}
	
	@Override
	public Void visitInferenceVar(final InferenceVar t, final P sb)
	{
		sb.append(t.getName());
		return null;
	}
	
	protected Void join(
		final P sb,
		final List<? extends JTypeMirror> ts,
		final String delim,
		final String prefix,
		final String suffix)
	{
		return this.join(sb, ts, delim, prefix, suffix, false);
	}
	
	protected Void join(
		final P sb,
		final List<? extends JTypeMirror> types,
		final String delim,
		final String prefix,
		final String suffix,
		final boolean isVarargs)
	{
		sb.isVarargs = false;
		sb.append(prefix);
		if(!types.isEmpty())
		{
			for(int i = 0; i < types.size() - 1; i++)
			{
				types.get(i).acceptVisitor(this, sb);
				sb.append(delim);
			}
			if(isVarargs)
			{
				sb.isVarargs = true;
			}
			types.get(types.size() - 1).acceptVisitor(this, sb);
		}
		sb.append(suffix);
		return null;
	}
}
