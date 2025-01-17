ruleset {

    description 'MGD CodeNarc RuleSet for Spock Tests'

    // rulesets/basic.xml
    // skipped: MultipleUnaryOperators
    // skipped: DoubleNegative
    AssertWithinFinallyBlock
    AssignmentInConditional
    BigDecimalInstantiation
    BitwiseOperatorInConditional
    BooleanGetBoolean
    BrokenNullCheck
    BrokenOddnessCheck
    ClassForName
    ComparisonOfTwoConstants
    ComparisonWithSelf
    ConstantAssertExpression
    ConstantIfExpression
    ConstantTernaryExpression
    DeadCode
    DuplicateCaseStatement
    DuplicateMapKey
    DuplicateSetValue
    EmptyCatchBlock
    EmptyClass
    EmptyElseBlock
    EmptyFinallyBlock
    EmptyForStatement
    EmptyIfStatement
    EmptyInstanceInitializer
    EmptyMethod
    EmptyStaticInitializer
    EmptySwitchStatement
    EmptySynchronizedStatement
    EmptyTryBlock
    EmptyWhileStatement
    EqualsAndHashCode
    EqualsOverloaded
    ExplicitGarbageCollection
    ForLoopShouldBeWhileLoop
    HardCodedWindowsFileSeparator
    HardCodedWindowsRootDirectory
    IntegerGetInteger
    ParameterAssignmentInFilterClosure
    RandomDoubleCoercedToZero
    RemoveAllOnSelf
    ReturnFromFinallyBlock
    ThrowExceptionFromFinallyBlock

    // rulesets/braces.xml
    ElseBlockBraces
    ForStatementBraces
    IfStatementBraces
    WhileStatementBraces

    // rulesets/comments.xml
    ClassJavadoc {
        doNotApplyToFileNames = '**/*Test.groovy'
    }
    JavadocConsecutiveEmptyLines
    JavadocEmptyAuthorTag
    JavadocEmptyExceptionTag
    JavadocEmptyFirstLine
    JavadocEmptyLastLine
    JavadocEmptyParamTag
    JavadocEmptyReturnTag
    JavadocEmptySeeTag
    JavadocEmptySinceTag
    JavadocEmptyThrowsTag
    JavadocEmptyVersionTag
    JavadocMissingExceptionDescription
    JavadocMissingParamDescription
    JavadocMissingThrowsDescription

    // rulesets/concurrency.xml
    BusyWait
    DoubleCheckedLocking
    InconsistentPropertyLocking
    InconsistentPropertySynchronization
    NestedSynchronization
    StaticCalendarField
    StaticConnection
    StaticDateFormatField
    StaticMatcherField
    StaticSimpleDateFormatField
    SynchronizedMethod
    SynchronizedOnBoxedPrimitive
    SynchronizedOnGetClass
    SynchronizedOnReentrantLock
    SynchronizedOnString
    SynchronizedOnThis
    SynchronizedReadObjectMethod
    SystemRunFinalizersOnExit
    ThisReferenceEscapesConstructor
    ThreadGroup
    ThreadLocalNotStaticFinal
    ThreadYield
    UseOfNotifyMethod
    VolatileArrayField
    VolatileLongOrDoubleField
    WaitOutsideOfWhileLoop

    // rulesets/convention.xml
    // skipped: CompileStatic
    // skipped: StaticFieldsBeforeInstanceFields
    // skipped: ImplicitClosureParameter
    // skipped: TrailingComma
    ConfusingTernary
    CouldBeElvis
    CouldBeSwitchStatement
    FieldTypeRequired
    HashtableIsObsolete
    IfStatementCouldBeTernary
    ImplicitReturnStatement
    InvertedCondition
    InvertedIfElse
    LongLiteralWithLowerCaseL
    MethodParameterTypeRequired
    MethodReturnTypeRequired {
        ignoreMethodNames = 'setup,setupSpec,cleanup,cleanupSpec,should*'
    }
    NoDef {
        excludeRegex = 'setup|setupSpec|cleanup|cleanupSpec|should .*'
    }
    NoDouble
    NoFloat
    NoJavaUtilDate
    NoTabCharacter
    ParameterReassignment
    PublicMethodsBeforeNonPublicMethods
    StaticMethodsBeforeInstanceMethods
    TernaryCouldBeElvis
    VariableTypeRequired
    VectorIsObsolete

    // rulesets/design.xml
    // skipped: AssignmentToStaticFieldFromInstanceMethod
    AbstractClassWithPublicConstructor
    AbstractClassWithoutAbstractMethod
    BooleanMethodReturnsNull
    BuilderMethodWithSideEffects {
        methodNameRegex = '(make.*|build.*)'
    }
    CloneableWithoutClone
    CloseWithoutCloseable
    CompareToWithoutComparable
    ConstantsOnlyInterface
    EmptyMethodInAbstractClass
    FinalClassWithProtectedMember
    ImplementationAsType
    Instanceof
    LocaleSetDefault
    NestedForLoop
    OptionalCollectionReturnType
    OptionalField
    OptionalMethodParameter
    PrivateFieldCouldBeFinal
    PublicInstanceField
    ReturnsNullInsteadOfEmptyArray
    ReturnsNullInsteadOfEmptyCollection
    SimpleDateFormatMissingLocale
    StatelessSingleton
    ToStringReturnsNull

    // rulesets/dry.xml
    // skipped: DuplicateStringLiteral
    // skipped: DuplicateNumberLiteral
    DuplicateListLiteral
    DuplicateMapLiteral

    // skipped: rulesets/enhanced.xml

    // rulesets/exceptions.xml
    // skipped: CatchException
    CatchArrayIndexOutOfBoundsException
    CatchError
    CatchIllegalMonitorStateException
    CatchIndexOutOfBoundsException
    CatchNullPointerException
    CatchRuntimeException
    CatchThrowable
    ConfusingClassNamedException
    ExceptionExtendsError
    ExceptionExtendsThrowable
    ExceptionNotThrown
    MissingNewInThrowStatement
    ReturnNullFromCatchBlock
    SwallowThreadDeath
    ThrowError
    ThrowException
    ThrowNullPointerException
    ThrowRuntimeException
    ThrowThrowable

    // rulesets/formatting.xml
    // skipped: BlockStartsWithBlankLine
    BlankLineBeforePackage
    BlockEndsWithBlankLine
    BracesForClass
    BracesForForLoop
    BracesForIfElse {
        validateElse = true
        elseOnSameLineAsClosingBrace = false
    }
    BracesForMethod
    BracesForTryCatchFinally
    ClassEndsWithBlankLine {
        ignoreInnerClasses = true
        blankLineRequired = false
    }
    ClassStartsWithBlankLine
    ClosureStatementOnOpeningLineOfMultipleLineClosure
    ConsecutiveBlankLines
    FileEndsWithoutNewline
    Indentation
    LineLength {
        length = 180
    }
    MissingBlankLineAfterImports
    MissingBlankLineAfterPackage
    MissingBlankLineBeforeAnnotatedField
    SpaceAfterCatch
    SpaceAfterClosingBrace
    SpaceAfterComma
    SpaceAfterFor
    SpaceAfterIf
    SpaceAfterMethodCallName
    SpaceAfterMethodDeclarationName
    SpaceAfterNotOperator
    SpaceAfterOpeningBrace {
        ignoreEmptyBlock = true
    }
    SpaceAfterSemicolon
    SpaceAfterSwitch
    SpaceAfterWhile
    SpaceAroundClosureArrow
    SpaceAroundMapEntryColon {
        characterAfterColonRegex = '\\s'
    }
    SpaceAroundOperator
    SpaceBeforeClosingBrace {
        ignoreEmptyBlock = true
    }
    SpaceBeforeOpeningBrace
    SpaceInsideParentheses
    TrailingWhitespace

    // rulesets/generic.xml
    IllegalClassMember
    IllegalClassReference
    IllegalPackageReference
    IllegalRegex
    IllegalString
    IllegalSubclass
    RequiredRegex
    RequiredString
    StatelessClass

    // skipped: rulesets/grails.xml

    // rulesets/groovyism.xml
    // skipped: GetterMethodCouldBeProperty
    // skipped: ClosureAsLastMethodParameter
    AssignCollectionSort
    AssignCollectionUnique
    CollectAllIsDeprecated
    ConfusingMultipleReturns
    ExplicitArrayListInstantiation
    ExplicitCallToAndMethod
    ExplicitCallToCompareToMethod
    ExplicitCallToDivMethod
    ExplicitCallToEqualsMethod
    ExplicitCallToGetAtMethod
    ExplicitCallToLeftShiftMethod
    ExplicitCallToMinusMethod
    ExplicitCallToModMethod
    ExplicitCallToMultiplyMethod
    ExplicitCallToOrMethod
    ExplicitCallToPlusMethod
    ExplicitCallToPowerMethod
    ExplicitCallToPutAtMethod
    ExplicitCallToRightShiftMethod
    ExplicitCallToXorMethod
    ExplicitHashMapInstantiation
    ExplicitHashSetInstantiation
    ExplicitLinkedHashMapInstantiation
    ExplicitLinkedListInstantiation
    ExplicitStackInstantiation
    ExplicitTreeSetInstantiation
    GStringAsMapKey
    GStringExpressionWithinString
    GroovyLangImmutable
    UseCollectMany
    UseCollectNested

    // rulesets/imports.xml
    // skipped: NoWildcardImports
    // skipped: MisorderedStaticImports
    DuplicateImport
    ImportFromSamePackage
    ImportFromSunPackages
    UnnecessaryGroovyImport
    UnusedImport

    // rulesets/jdbc.xml
    DirectConnectionManagement
    JdbcConnectionReference
    JdbcResultSetReference
    JdbcStatementReference

    // rulesets/junit.xml
    // skipped: JUnitTestMethodWithoutAssert
    // skipped: JUnitPublicNonTestMethod
    ChainedTest
    CoupledTestCase
    JUnitAssertAlwaysFails
    JUnitAssertAlwaysSucceeds
    JUnitFailWithoutMessage
    JUnitLostTest
    JUnitPublicField
    JUnitPublicProperty
    JUnitSetUpCallsSuper
    JUnitStyleAssertions
    JUnitTearDownCallsSuper
    JUnitUnnecessarySetUp
    JUnitUnnecessaryTearDown
    JUnitUnnecessaryThrowsException
    SpockIgnoreRestUsed
    UnnecessaryFail
    UseAssertEqualsInsteadOfAssertTrue
    UseAssertFalseInsteadOfNegation
    UseAssertNullInsteadOfAssertEquals
    UseAssertSameInsteadOfAssertTrue
    UseAssertTrueInsteadOfAssertEquals
    UseAssertTrueInsteadOfNegation

    // rulesets/logging.xml
    LoggerForDifferentClass
    LoggerWithWrongModifiers
    LoggingSwallowsStacktrace
    MultipleLoggers
    PrintStackTrace
    Println
    SystemErrPrint
    SystemOutPrint

    // rulesets/naming.xml
    // skipped: FactoryMethodName
    AbstractClassName
    ClassName
    ClassNameSameAsFilename
    ClassNameSameAsSuperclass
    ConfusingMethodName
    FieldName {
        finalRegex = '[a-z][a-zA-Z0-9]*'
        staticRegex = '[a-z][a-zA-Z0-9]*'
        staticFinalRegex = '[A-Z][A-Z0-9_]*|log|map'
        ignoreFieldNames = 'MGD_*,SQS_*,RETRY_*,MAX_*'
    }
    InterfaceName
    InterfaceNameSameAsSuperInterface
    MethodName {
        ignoreMethodNames = 'should*'
    }
    ObjectOverrideMisspelledMethodName
    PackageName
    PackageNameMatchesFilePath
    ParameterName
    PropertyName
    VariableName

    // rulesets/security.xml
    // skipped: JavaIoPackageAccess
    FileCreateTempFile
    InsecureRandom
    NonFinalPublicField
    NonFinalSubclassOfSensitiveInterface
    ObjectFinalize
    PublicFinalizeMethod
    SystemExit
    UnsafeArrayDeclaration

    // rulesets/serialization.xml
    EnumCustomSerializationIgnored
    SerialPersistentFields
    SerialVersionUID
    SerializableClassMustDefineSerialVersionUID

    // rulesets/size.xml
    // skipped: AbcMetric   // Requires the GMetrics jar
    // skipped: CrapMetric   // Requires the GMetrics jar and a Cobertura coverage file
    // skipped: CyclomaticComplexity   // Requires the GMetrics jar
    // skipped: MethodCount   // Does not ignore getters and setters in counts
    ClassSize
    MethodSize
    NestedBlockDepth
    ParameterCount {
        maxParameters = 10
    }

    // rulesets/unnecessary.xml
    // skipped: UnnecessaryReturnKeyword
    // skipped: UnnecessarySetter
    // skipped: UnnecessaryOverridingMethod
    AddEmptyString
    ConsecutiveLiteralAppends
    ConsecutiveStringConcatenation
    UnnecessaryBigDecimalInstantiation
    UnnecessaryBigIntegerInstantiation
    UnnecessaryBooleanExpression
    UnnecessaryBooleanInstantiation
    UnnecessaryCallForLastElement
    UnnecessaryCallToSubstring
    UnnecessaryCast
    UnnecessaryCatchBlock
    UnnecessaryCollectCall
    UnnecessaryCollectionCall
    UnnecessaryConstructor
    UnnecessaryDefInFieldDeclaration
    UnnecessaryDefInMethodDeclaration
    UnnecessaryDefInVariableDeclaration
    UnnecessaryDotClass
    UnnecessaryDoubleInstantiation
    UnnecessaryElseStatement
    UnnecessaryFinalOnPrivateMethod
    UnnecessaryFloatInstantiation
    UnnecessaryGString
    UnnecessaryGetter {
        ignoreMethodNames = 'getProperties'
        checkIsMethods = false
    }
    UnnecessaryIfStatement
    UnnecessaryInstanceOfCheck
    UnnecessaryInstantiationToGetClass
    UnnecessaryIntegerInstantiation
    UnnecessaryLongInstantiation
    UnnecessaryModOne
    UnnecessaryNullCheck
    UnnecessaryNullCheckBeforeInstanceOf
    UnnecessaryObjectReferences
    UnnecessaryPackageReference
    UnnecessaryParenthesesForMethodCallWithClosure
    UnnecessaryPublicModifier
    UnnecessarySafeNavigationOperator
    UnnecessarySelfAssignment
    UnnecessarySemicolon
    UnnecessaryStringInstantiation
    UnnecessaryTernaryExpression
    UnnecessaryToString
    UnnecessaryTransientModifier

    // rulesets/unused.xml
    UnusedArray
    UnusedMethodParameter {
        ignoreRegex = 'ignored|event'
    }
    UnusedObject
    UnusedPrivateField
    UnusedPrivateMethod
    UnusedPrivateMethodParameter
    UnusedVariable
}