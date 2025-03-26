package delta.games.lotro.values.codec;

import java.util.Arrays;
import java.util.BitSet;

import delta.common.utils.NumericTools;
import delta.games.lotro.values.ArrayValue;
import delta.games.lotro.values.EnumValue;
import delta.games.lotro.values.StructValue;
import delta.games.lotro.values.utils.StringReader;

/**
 * Reads values from strings.
 * @author DAM
 */
public class ValueReader
{
  /**
   * Read a value from a string.
   * @param value Input string.
   * @return the loaded value.
   */
  public static Object read(String value)
  {
    if (value==null)
    {
      return null;
    }
    if (value.isEmpty())
    {
      return null;
    }
    StringReader r=new StringReader(value);
    return new ValueReader().read(r);
  }

  private Object read(StringReader r)
  {
    char firstChar=r.peek();
    if (firstChar=='\'')
    {
      return readStringValue(r);
    }
    else if (firstChar=='[')
    {
      return readArrayValue(r);
    }
    else if (firstChar=='{')
    {
      return readStructValue(r);
    }
    else if (firstChar=='(')
    {
      return readBitSet(r);
    }
    else if (firstChar=='e')
    {
      return readEnumValue(r);
    }
    else if (firstChar=='n')
    {
      r.readExpectedString("null");
      return null;
    }
    else if (firstChar=='t')
    {
      r.readExpectedString("true");
      return Boolean.TRUE;
    }
    else if (firstChar=='f')
    {
      r.readExpectedString("false");
      return Boolean.FALSE;
    }
    return readNumber(r);
  }

  private String readStringValue(StringReader r)
  {
    r.readExpectedChar('\'');
    StringBuilder sb=new StringBuilder();
    while (true)
    {
      char nextChar=r.peek();
      if (nextChar=='\\')
      {
        r.skip(1);
        sb.append(r.readChar());
      }
      else if (nextChar=='\'')
      {
        r.skip(1);
        break;
      }
      else
      {
        sb.append(r.readChar());
      }
    }
    return sb.toString();
  }

  private ArrayValue readArrayValue(StringReader r)
  {
    r.readExpectedChar('[');
    ArrayValue ret=new ArrayValue();
    while(true)
    {
      char nextChar=r.peek();
      if (nextChar==',')
      {
        r.readExpectedChar(',');
      }
      if (nextChar!=']')
      {
        Object childValue=read(r);
        ret.addValue(childValue);
      }
      else
      {
        break;
      }
    }
    r.readExpectedChar(']');
    return ret;
  }

  private StructValue readStructValue(StringReader r)
  {
    r.readExpectedChar('{');
    StructValue ret=new StructValue();
    while(true)
    {
      char nextChar=r.peek();
      if (nextChar==',')
      {
        r.readExpectedChar(',');
      }
      if (nextChar!='}')
      {
        String key=readStringValue(r);
        r.readExpectedChar(':');
        Object childValue=read(r);
        ret.setValue(key,childValue);
      }
      else
      {
        break;
      }
    }
    r.readExpectedChar('}');
    return ret;
  }

  private BitSet readBitSet(StringReader r)
  {
    r.readExpectedChar('(');
    BitSet ret=new BitSet();
    while(true)
    {
      char nextChar=r.peek();
      if (nextChar==',')
      {
        r.readExpectedChar(',');
      }
      if (nextChar!=')')
      {
        int code=readInt(r);
        ret.set(code);
      }
      else
      {
        break;
      }
    }
    r.readExpectedChar(')');
    return ret;

  }

  private EnumValue readEnumValue(StringReader r)
  {
    r.readExpectedChar('e');
    int enumId=readInt(r);
    r.readExpectedChar(':');
    int value=readInt(r);
    EnumValue ret=new EnumValue();
    ret.setEnumId(enumId);
    ret.setValue(value);
    return ret;
  }

  private Number readNumber(StringReader r)
  {
    char[] allowedChars="0123456789-.".toCharArray();
    Arrays.sort(allowedChars);
    String rawValue=r.readAllowedChars(allowedChars);
    if (rawValue.isEmpty())
    {
      throw new IllegalStateException("Could not read a number!");
    }
    boolean isFloat=false;
    boolean isLong=false;
    boolean end=r.isAtEnd();
    if (!end)
    {
      char next=r.peek();
      if (next=='f')
      {
        isFloat=true;
      }
      else if (next=='L')
      {
        isLong=true;
      }
    }
    Number ret=null;
    if (isLong)
    {
      ret=NumericTools.parseLong(rawValue);
      r.readExpectedChar('L');
    }
    else if (isFloat)
    {
      ret=NumericTools.parseFloat(rawValue);
      r.readExpectedChar('f');
    }
    else
    {
      ret=NumericTools.parseInteger(rawValue);
    }
    return ret;
  }

  private int readInt(StringReader r)
  {
    int ret=0;
    int nbDigits=0;
    while(true)
    {
      if (r.isAtEnd())
      {
        break;
      }
      char c=r.peek();
      if ((c>='0') && (c<='9'))
      {
        ret=ret*10+(c-'0');
        nbDigits++;
        r.skip(1);
      }
      else
      {
        break;
      }
    }
    if (nbDigits==0)
    {
      throw new IllegalStateException("Integer with no digits!");
    }
    return ret;
  }
}
