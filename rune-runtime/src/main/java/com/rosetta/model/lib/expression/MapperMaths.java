package com.rosetta.model.lib.expression;

import com.rosetta.model.lib.mapper.Mapper;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

@SuppressWarnings("all")
public class MapperMaths {
  public static <R extends Object, A extends Object, B extends Object> MapperS<R> add(final Mapper<? extends A> in1, final Mapper<? extends B> in2) {
    if (((in1.resultCount() == 1) && (in2.resultCount() == 1))) {
      final A arg1 = in1.get();
      final B arg2 = in2.get();
      Object _plus = MapperMaths.operator_plus(arg1, arg2);
      return MapperS.<R>of(((R) _plus));
    }
    return MapperS.<R>ofNull();
  }

  public static <R extends Object, A extends Object, B extends Object> MapperS<R> subtract(final Mapper<? extends A> in1, final Mapper<? extends B> in2) {
    if (((in1.resultCount() == 1) && (in2.resultCount() == 1))) {
      final A arg1 = in1.get();
      final B arg2 = in2.get();
      Object _minus = MapperMaths.operator_minus(arg1, arg2);
      return MapperS.<R>of(((R) _minus));
    }
    return MapperS.<R>ofNull();
  }

  public static <R extends Object, A extends Object, B extends Object> MapperS<R> multiply(final Mapper<? extends A> in1, final Mapper<? extends B> in2) {
    if (((in1.resultCount() == 1) && (in2.resultCount() == 1))) {
      final A arg1 = in1.get();
      final B arg2 = in2.get();
      Object _multiply = MapperMaths.operator_multiply(arg1, arg2);
      return MapperS.<R>of(((R) _multiply));
    }
    return MapperS.<R>ofNull();
  }

  public static <R extends Object, A extends Object, B extends Object> MapperS<R> divide(final Mapper<? extends A> in1, final Mapper<? extends B> in2) {
    if (((in1.resultCount() == 1) && (in2.resultCount() == 1))) {
      final A arg1 = in1.get();
      final B arg2 = in2.get();
      Object _divide = MapperMaths.operator_divide(arg1, arg2);
      return MapperS.<R>of(((R) _divide));
    }
    return MapperS.<R>ofNull();
  }

  private static Object _operator_plus(final Object a, final Object b) {
    StringBuilder _builder = new StringBuilder();
    _builder.append("Cant add two random (");
    String _simpleName = a.getClass().getSimpleName();
    _builder.append(_simpleName);
    _builder.append(", ");
    String _simpleName_1 = b.getClass().getSimpleName();
    _builder.append(_simpleName_1);
    _builder.append(") together");
    throw new RuntimeException(_builder.toString());
  }

  private static BigDecimal _operator_plus(final LocalDate d1, final LocalDate d2) {
    StringBuilder _builder = new StringBuilder();
    _builder.append("Cant add two dates together");
    throw new RuntimeException(_builder.toString());
  }

  private static LocalDateTime _operator_plus(final LocalDate d, final LocalTime t) {
    return LocalDateTime.of(d, t);
  }

  private static String _operator_plus(final String a, final String b) {
    return (a + b);
  }

  private static Integer _operator_plus(final Integer a, final Integer b) {
    int _intValue = a.intValue();
    int _intValue_1 = b.intValue();
    return Integer.valueOf((_intValue + _intValue_1));
  }

  private static Long _operator_plus(final Long a, final Long b) {
    long _longValue = a.longValue();
    long _longValue_1 = b.longValue();
    return Long.valueOf((_longValue + _longValue_1));
  }

  private static BigInteger _operator_plus(final BigInteger a, final BigInteger b) {
    return a.add(b);
  }

  private static BigDecimal _operator_plus(final Number a, final Number b) {
    final BigDecimal bigA = MapperMaths.toBigD(a);
    final BigDecimal bigB = MapperMaths.toBigD(b);
    return bigA.add(bigB);
  }

  private static Object _operator_minus(final Object a, final Object b) {
    StringBuilder _builder = new StringBuilder();
    _builder.append("Cant subtract two random (");
    String _simpleName = a.getClass().getSimpleName();
    _builder.append(_simpleName);
    _builder.append(", ");
    String _simpleName_1 = b.getClass().getSimpleName();
    _builder.append(_simpleName_1);
    _builder.append(") together");
    throw new RuntimeException(_builder.toString());
  }

  private static Integer _operator_minus(final LocalDate d1, final LocalDate d2) {
    return Integer.valueOf(Long.valueOf(ChronoUnit.DAYS.between(d2, d1)).intValue());
  }

  private static LocalDateTime _operator_minus(final LocalDate d, final LocalTime t) {
    StringBuilder _builder = new StringBuilder();
    _builder.append("Cant subtract time from date");
    throw new RuntimeException(_builder.toString());
  }

  private static String _operator_minus(final String a, final String b) {
    StringBuilder _builder = new StringBuilder();
    _builder.append("Cant subtract two strings together");
    throw new RuntimeException(_builder.toString());
  }

  private static Integer _operator_minus(final Integer a, final Integer b) {
    int _intValue = a.intValue();
    int _intValue_1 = b.intValue();
    return Integer.valueOf((_intValue - _intValue_1));
  }

  private static Long _operator_minus(final Long a, final Long b) {
    long _longValue = a.longValue();
    long _longValue_1 = b.longValue();
    return Long.valueOf((_longValue - _longValue_1));
  }

  private static BigInteger _operator_minus(final BigInteger a, final BigInteger b) {
    return a.subtract(b);
  }

  private static BigDecimal _operator_minus(final Number a, final Number b) {
    final BigDecimal bigA = MapperMaths.toBigD(a);
    final BigDecimal bigB = MapperMaths.toBigD(b);
    return bigA.subtract(bigB);
  }

  private static Object _operator_multiply(final Object a, final Object b) {
    StringBuilder _builder = new StringBuilder();
    _builder.append("Cant multiply two random (");
    String _simpleName = a.getClass().getSimpleName();
    _builder.append(_simpleName);
    _builder.append(", ");
    String _simpleName_1 = b.getClass().getSimpleName();
    _builder.append(_simpleName_1);
    _builder.append(") together");
    throw new RuntimeException(_builder.toString());
  }

  private static BigDecimal _operator_multiply(final LocalDate d1, final LocalDate d2) {
    StringBuilder _builder = new StringBuilder();
    _builder.append("Cant multiply date and date");
    throw new RuntimeException(_builder.toString());
  }

  private static LocalDateTime _operator_multiply(final LocalDate d, final LocalTime t) {
    StringBuilder _builder = new StringBuilder();
    _builder.append("Cant multiply time and date");
    throw new RuntimeException(_builder.toString());
  }

  private static String _operator_multiply(final String a, final String b) {
    StringBuilder _builder = new StringBuilder();
    _builder.append("Cant multiply two strings together");
    throw new RuntimeException(_builder.toString());
  }

  private static Integer _operator_multiply(final Integer a, final Integer b) {
    int _intValue = a.intValue();
    int _intValue_1 = b.intValue();
    return Integer.valueOf((_intValue * _intValue_1));
  }

  private static Long _operator_multiply(final Long a, final Long b) {
    long _longValue = a.longValue();
    long _longValue_1 = b.longValue();
    return Long.valueOf((_longValue * _longValue_1));
  }

  private static BigInteger _operator_multiply(final BigInteger a, final BigInteger b) {
    return a.multiply(b);
  }

  private static BigDecimal _operator_multiply(final Number a, final Number b) {
    final BigDecimal bigA = MapperMaths.toBigD(a);
    final BigDecimal bigB = MapperMaths.toBigD(b);
    return bigA.multiply(bigB);
  }

  private static Object _operator_divide(final Object a, final Object b) {
    StringBuilder _builder = new StringBuilder();
    _builder.append("Cant divide two random (");
    String _simpleName = a.getClass().getSimpleName();
    _builder.append(_simpleName);
    _builder.append(", ");
    String _simpleName_1 = b.getClass().getSimpleName();
    _builder.append(_simpleName_1);
    _builder.append(")");
    throw new RuntimeException(_builder.toString());
  }

  private static BigDecimal _operator_divide(final LocalDate d1, final LocalDate d2) {
    StringBuilder _builder = new StringBuilder();
    _builder.append("Cant divide date and date");
    throw new RuntimeException(_builder.toString());
  }

  private static LocalDateTime _operator_divide(final LocalDate d, final LocalTime t) {
    StringBuilder _builder = new StringBuilder();
    _builder.append("Cant divide time and date");
    throw new RuntimeException(_builder.toString());
  }

  private static String _operator_divide(final String a, final String b) {
    StringBuilder _builder = new StringBuilder();
    _builder.append("Cant divide two strings");
    throw new RuntimeException(_builder.toString());
  }

  private static BigDecimal _operator_divide(final Number a, final Number b) {
    final BigDecimal bigA = MapperMaths.toBigD(a);
    final BigDecimal bigB = MapperMaths.toBigD(b);
    return bigA.divide(bigB, MathContext.DECIMAL128);
  }

  public static BigDecimal toBigD(final Number n) {
    boolean _matched = false;
    if (n instanceof BigDecimal) {
      _matched=true;
      return ((BigDecimal)n);
    }
    if (!_matched) {
      if (n instanceof BigInteger) {
        _matched=true;
        return new BigDecimal(((BigInteger)n));
      }
    }
    if (!_matched) {
      if (n instanceof Long) {
        _matched=true;
        long _longValue = ((Long)n).longValue();
        return new BigDecimal(_longValue);
      }
    }
    if (!_matched) {
      if (n instanceof Integer) {
        _matched=true;
        int _intValue = ((Integer)n).intValue();
        return new BigDecimal(_intValue);
      }
    }
    return null;
  }

  private static Object operator_plus(final Object d1, final Object d2) {
    if (d1 instanceof LocalDate
         && d2 instanceof LocalDate) {
      return _operator_plus((LocalDate)d1, (LocalDate)d2);
    } else if (d1 instanceof LocalDate
         && d2 instanceof LocalTime) {
      return _operator_plus((LocalDate)d1, (LocalTime)d2);
    } else if (d1 instanceof Integer
         && d2 instanceof Integer) {
      return _operator_plus((Integer)d1, (Integer)d2);
    } else if (d1 instanceof Long
         && d2 instanceof Long) {
      return _operator_plus((Long)d1, (Long)d2);
    } else if (d1 instanceof BigInteger
         && d2 instanceof BigInteger) {
      return _operator_plus((BigInteger)d1, (BigInteger)d2);
    } else if (d1 instanceof Number
         && d2 instanceof Number) {
      return _operator_plus((Number)d1, (Number)d2);
    } else if (d1 instanceof String
         && d2 instanceof String) {
      return _operator_plus((String)d1, (String)d2);
    } else if (d1 != null
         && d2 != null) {
      return _operator_plus(d1, d2);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(d1, d2).toString());
    }
  }

  private static Object operator_minus(final Object d1, final Object d2) {
    if (d1 instanceof LocalDate
         && d2 instanceof LocalDate) {
      return _operator_minus((LocalDate)d1, (LocalDate)d2);
    } else if (d1 instanceof LocalDate
         && d2 instanceof LocalTime) {
      return _operator_minus((LocalDate)d1, (LocalTime)d2);
    } else if (d1 instanceof Integer
         && d2 instanceof Integer) {
      return _operator_minus((Integer)d1, (Integer)d2);
    } else if (d1 instanceof Long
         && d2 instanceof Long) {
      return _operator_minus((Long)d1, (Long)d2);
    } else if (d1 instanceof BigInteger
         && d2 instanceof BigInteger) {
      return _operator_minus((BigInteger)d1, (BigInteger)d2);
    } else if (d1 instanceof Number
         && d2 instanceof Number) {
      return _operator_minus((Number)d1, (Number)d2);
    } else if (d1 instanceof String
         && d2 instanceof String) {
      return _operator_minus((String)d1, (String)d2);
    } else if (d1 != null
         && d2 != null) {
      return _operator_minus(d1, d2);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(d1, d2).toString());
    }
  }

  private static Object operator_multiply(final Object d1, final Object d2) {
    if (d1 instanceof LocalDate
         && d2 instanceof LocalDate) {
      return _operator_multiply((LocalDate)d1, (LocalDate)d2);
    } else if (d1 instanceof LocalDate
         && d2 instanceof LocalTime) {
      return _operator_multiply((LocalDate)d1, (LocalTime)d2);
    } else if (d1 instanceof Integer
         && d2 instanceof Integer) {
      return _operator_multiply((Integer)d1, (Integer)d2);
    } else if (d1 instanceof Long
         && d2 instanceof Long) {
      return _operator_multiply((Long)d1, (Long)d2);
    } else if (d1 instanceof BigInteger
         && d2 instanceof BigInteger) {
      return _operator_multiply((BigInteger)d1, (BigInteger)d2);
    } else if (d1 instanceof Number
         && d2 instanceof Number) {
      return _operator_multiply((Number)d1, (Number)d2);
    } else if (d1 instanceof String
         && d2 instanceof String) {
      return _operator_multiply((String)d1, (String)d2);
    } else if (d1 != null
         && d2 != null) {
      return _operator_multiply(d1, d2);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(d1, d2).toString());
    }
  }

  private static Object operator_divide(final Object d1, final Object d2) {
    if (d1 instanceof LocalDate
         && d2 instanceof LocalDate) {
      return _operator_divide((LocalDate)d1, (LocalDate)d2);
    } else if (d1 instanceof LocalDate
         && d2 instanceof LocalTime) {
      return _operator_divide((LocalDate)d1, (LocalTime)d2);
    } else if (d1 instanceof Number
         && d2 instanceof Number) {
      return _operator_divide((Number)d1, (Number)d2);
    } else if (d1 instanceof String
         && d2 instanceof String) {
      return _operator_divide((String)d1, (String)d2);
    } else if (d1 != null
         && d2 != null) {
      return _operator_divide(d1, d2);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(d1, d2).toString());
    }
  }
}
