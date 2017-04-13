package ingraph.relalg.calculators

import ingraph.logger.IngraphLogger
import ingraph.relalg.collectors.CollectionHelper
import java.util.List
import relalg.AbstractJoinOperator
import relalg.NullaryOperator
import relalg.ProjectionOperator
import relalg.RelalgContainer
import relalg.UnaryOperator
import relalg.UnionOperator
import relalg.Variable

/**
 * Calculates extra variables.
 * 
 * For example, a projection may need extra variables for projecting attributes
 * or a selection may need extra attributes for evaluating conditions.
 */
class ExtraVariablesCalculator {

	extension IngraphLogger logger = new IngraphLogger(ExtraVariablesCalculator.name)
	extension VariableExtractor variableExtractor = new VariableExtractor
	extension CollectionHelper listUnionCalculator = new CollectionHelper
	extension ExtraVariablesPropagator extraVariablesPropagator = new ExtraVariablesPropagator

	def calculateExtraVariables(RelalgContainer container) {
		if (!container.incrementalPlan) {
			throw new IllegalStateException("ExtraVariablesCalculator must be executed on an incremental query plan")
		}

		if (!container.isExternalSchemaInferred) {
			throw new IllegalStateException("ExternalSchemaCalculator must be executed before ExtraVariableCalculator")
		} else if (container.isExtraVariablesInferred) {
			throw new IllegalStateException("ExtraVariablesCalculator on relalg container was already executed")
		} else {
			container.extraVariablesInferred = true
		}

		container.rootExpression.fillExtraVariables(#[])
		container
	}

	/*
	 * nullary operators
	 */
	private def dispatch void fillExtraVariables(NullaryOperator op, List<Variable> extraVariables) {
		op.extraVariables.addAll(extraVariables)
	}

	/*
	 * unary operators
	 * 
	 * some unary operators, such as selection, projection and grouping, often require extra variables
	 */
	private def dispatch void fillExtraVariables(UnaryOperator op, List<Variable> extraVariables) {
		op.extraVariables.addAll(extraVariables)

		val newExtraVariables = extractUnaryOperatorExtraVariables(op)
		var inputExtraVariables = uniqueUnion(extraVariables, newExtraVariables)

		if (op instanceof ProjectionOperator) {
			inputExtraVariables = inputExtraVariables.minus(op.calculatedVariables)
		}

		val filteredInputExtraVariables = inputExtraVariables.propagateTo(op.input)
		op.input.fillExtraVariables(filteredInputExtraVariables)
	}

	/*
	 * binary operators 
	 */
	private def dispatch void fillExtraVariables(UnionOperator op, List<Variable> extraVariables) {
		op.extraVariables.addAll(extraVariables)
		op.leftInput.fillExtraVariables(extraVariables)
		op.rightInput.fillExtraVariables(extraVariables)
	}

	private def dispatch void fillExtraVariables(AbstractJoinOperator op, List<Variable> extraVariables) {
		op.extraVariables.addAll(extraVariables)
		val leftExtraVariables = extraVariables.propagateTo(op.leftInput)
		val rightExtraVariables = extraVariables.propagateTo(op.rightInput)

		// remove duplicates as we only need each extra variable once
		// we choose "right\left" as it works for both equijoin and antijoin operators,
		// as extra attributes that are available from both the left and right input
		rightExtraVariables.removeAll(leftExtraVariables)

		op.leftInput.fillExtraVariables(leftExtraVariables)
		op.rightInput.fillExtraVariables(rightExtraVariables)
	}

}