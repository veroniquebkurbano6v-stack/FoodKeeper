import localUnitTest from './LocalUnit.test';
import agentRecipeTest from './AgentRecipe.test';

export default function testsuite() {
  localUnitTest();
  agentRecipeTest();
}