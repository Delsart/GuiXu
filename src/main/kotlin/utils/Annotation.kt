package work.delsart.guixu.utils


@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR, message =
        "This is a dangerous operation. make sure you understand what will happen."
)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.BINARY)
annotation class DangerousOperation


@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR, message =
        "Direct usage is not recommended. It may cause unexpected problems, make sure you fully understand what will happen."
)
@Target(
    AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY, AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR
)
@Retention(AnnotationRetention.BINARY)
annotation class DoNotUseDirectly