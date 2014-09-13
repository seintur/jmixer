JMixer
======

JMixer is the implementation of a mixin mechanism for the Java language.

The `@Mixin` annotation is defined to enable to mix in the annotated class the
code of the classes referenced in the parameter of the annotation. As an
example, the following code mixes in the `Duck` class the code from the
`Flying` and `Swimming` classes.

```
@Mixin(value={Flying.class, Swimming.class})
public class Duck extends Bird {}
public class Bird {}

public class Flying {
  public void fly() {}
}

public class Swimming{
  public void swim() {}
}
```

The mixin mechanism can be seen as a replacement for multiple inheritance.

Author: Lionel Seinturier
