import edu.berkeley.compbio.jlibsvm.LabelParser ;

public class MyLabelParser implements LabelParser<String> {

  public String parse(final String s)
  {
    return s;
  }
};
