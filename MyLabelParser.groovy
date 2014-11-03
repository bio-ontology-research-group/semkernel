import edu.berkeley.compbio.jlibsvm.LabelParser ;

public class MyLabelParser implements LabelParser<String> {

  public String parse(String s)
  {
    return s;
  }
};
