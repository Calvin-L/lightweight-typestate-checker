import org.lwtsc.qual.NewStateOnReturn;
import org.lwtsc.qual.RequiresState;
import org.lwtsc.qual.EnsuresState;

public class Postconditions {

  public void noEnsures(Job j) {
  }

  @EnsuresState(value="#1", state="complete")
  public void correctEnsures(Job j) {
    j.run();
  }

  @EnsuresState(value="#1", state="complete")
  // :: error: (contracts.postcondition)
  public void incorrectEnsures(Job j) { }

  @EnsuresState(value="#1", state="complete")
  public void ensuresOnlyMattersForNormalReturn(Job j) {
    throw new UnsupportedOperationException();
  }

  public void noEnsuresDoesNotPromiseIncomplete() {
    Job j = new Job();
    noEnsures(j);
    // :: error: (contracts.precondition)
    j.verifyComplete();
  }

  public void noEnsuresDoesNotPromiseComplete() {
    Job j = new Job();
    noEnsures(j);
    // :: error: (contracts.precondition)
    j.verifyComplete();
    // :: error: (contracts.precondition)
    j.verifyIncomplete();
  }

  public void useCorrectEnsures() {
    Job j = new Job();
    correctEnsures(j);
    j.verifyComplete();
  }

  static class Job {

    @NewStateOnReturn("incomplete")
    public Job() {
    }

    @NewStateOnReturn("complete")
    public void run() {
    }

    @RequiresState(value="this", state="incomplete")
    public void verifyIncomplete() {
    }

    @RequiresState(value="this", state="complete")
    public void verifyComplete() {
    }

  }

}
