/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package software.xdev.pmd.rule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.sourceforge.pmd.lang.java.ast.ASTClassDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTLocalVariableDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclarator;
import net.sourceforge.pmd.lang.java.ast.ASTVariableId;
import net.sourceforge.pmd.lang.java.ast.JavaNode;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRulechainRule;
import net.sourceforge.pmd.lang.java.types.JClassType;
import net.sourceforge.pmd.properties.PropertyDescriptor;
import net.sourceforge.pmd.properties.PropertyFactory;
import software.xdev.pmd.rule.prettyprint.PrettyPrintVisitor;
import software.xdev.pmd.rule.prettyprint.TypePrettyPrinter;


// Upstream code from https://github.com/pmd/pmd/pull/6904
public class AvoidVarForShortTypeRule extends AbstractJavaRulechainRule
{
	private static final PropertyDescriptor<Integer> LIMIT_LENGTH_DESCRIPTOR =
		PropertyFactory.intProperty("lengthLimit")
			.desc("The length of a type must be above this limit to allow using var")
			.defaultValue(32)
			.build();
	
	public AvoidVarForShortTypeRule()
	{
		super(ASTLocalVariableDeclaration.class);
		this.definePropertyDescriptor(LIMIT_LENGTH_DESCRIPTOR);
	}
	
	@Override
	public Object visit(final ASTLocalVariableDeclaration node, final Object data)
	{
		if(!node.isTypeInferred())
		{
			// not var
			return data;
		}
		
		final List<String> currentClassDeclarationCanonicalPrefixes =
			this.computeCurrentClassDeclarationCanonicalPrefixes(node);
		
		final int limitLength = this.getProperty(LIMIT_LENGTH_DESCRIPTOR);
		Optional.ofNullable(node.firstChild(ASTVariableDeclarator.class))
			.map(n -> n.firstChild(ASTVariableId.class))
			.map(ASTVariableId::getTypeMirror)
			.map(variableTypeMirror -> {
				final VisitState state = new VisitState(limitLength, currentClassDeclarationCanonicalPrefixes);
				try
				{
					variableTypeMirror.acceptVisitor(Visitor.INSTANCE, state);
				}
				catch(final OverLengthLimitException ignored)
				{
					// Quick exit/abort to not compute full string length
					return null;
				}
				return state.consumeResult();
			})
			.filter(s -> s.length() < limitLength)
			.ifPresent(type -> this.asCtx(data).addViolation(node, type, limitLength));
		
		return data;
	}
	
	private List<String> computeCurrentClassDeclarationCanonicalPrefixes(final ASTLocalVariableDeclaration node)
	{
		final List<String> currentClassDeclarationCanonicalPrefixes = new ArrayList<>();
		final Set<JavaNode> alreadyProcessed = new HashSet<>();
		
		JavaNode parent = node.getParent();
		while(parent != null && !alreadyProcessed.contains(parent))
		{
			if(parent instanceof final ASTClassDeclaration classDeclaration)
			{
				Optional.ofNullable(classDeclaration.getCanonicalName())
					.map(s -> s + ".")
					.ifPresent(currentClassDeclarationCanonicalPrefixes::add);
			}
			alreadyProcessed.add(parent);
			parent = parent.getParent();
		}
		
		return currentClassDeclarationCanonicalPrefixes;
	}
	
	static class OverLengthLimitException extends RuntimeException
	{
	
	}
	
	
	static class VisitState extends TypePrettyPrinter
	{
		final int limitLength;
		final List<String> currentClassDeclarationCanonicalPrefixes;
		
		int currentLength;
		
		VisitState(
			final int limitLength,
			final List<String> currentClassDeclarationCanonicalPrefixes)
		{
			this.limitLength = limitLength;
			this.currentClassDeclarationCanonicalPrefixes = currentClassDeclarationCanonicalPrefixes;
			
			this.printMethodHeader(false);
			this.printMethodResult(false);
			this.printAnnotations(false);
		}
		
		void throwIfCurrentLengthOverLimit()
		{
			if(this.currentLength >= this.limitLength)
			{
				throw new OverLengthLimitException();
			}
		}
		
		@Override
		public StringBuilder append(final char o)
		{
			this.currentLength++;
			this.throwIfCurrentLengthOverLimit();
			return super.append(o);
		}
		
		@Override
		public StringBuilder append(final String o)
		{
			this.currentLength += o.length();
			this.throwIfCurrentLengthOverLimit();
			return super.append(o);
		}
	}
	
	
	static class Visitor extends PrettyPrintVisitor<VisitState>
	{
		static final Visitor INSTANCE = new Visitor();
		
		@Override
		protected void appendClassName(
			final JClassType t,
			final VisitState s,
			final JClassType enclosing,
			final boolean isAnon)
		{
			final String canonicalName = t.getSymbol().getCanonicalName();
			if(canonicalName == null)
			{
				return;
			}
			
			if(!s.currentClassDeclarationCanonicalPrefixes.isEmpty())
			{
				// last -> top-most
				final String topMostCurrentClassDeclarationBinaryName =
					s.currentClassDeclarationCanonicalPrefixes.get(
						s.currentClassDeclarationCanonicalPrefixes.size() - 1);
				if(canonicalName.startsWith(topMostCurrentClassDeclarationBinaryName))
				{
					final Optional<String> optMostMatchedCanonicalClassPrefix =
						s.currentClassDeclarationCanonicalPrefixes.stream()
							.filter(canonicalName::startsWith)
							.findFirst();
					if(optMostMatchedCanonicalClassPrefix.isPresent())
					{
						s.append(canonicalName.substring(optMostMatchedCanonicalClassPrefix.orElseThrow().length()));
						return;
					}
				}
			}
			
			final String packageName = t.getSymbol().getPackageName();
			s.append(canonicalName.startsWith(packageName) && canonicalName.length() > packageName.length()
				? canonicalName.substring(packageName.length() + 1)
				: canonicalName);
		}
	}
}
