# Compiler - Alpha

Compiles to simplified assembly used by virtual machine with 6 registers a-f.

## Build
```bash
# run from project
mvn -q exec:java -Dexec.args="<infile> <outfile>"

# build .jar
mvn assembly:single
# run from .jar
java -jar Compiler.jar <infile> <outfile>
```

## Grammar
- Program structure
    - With declaration
    ```java
    DECLARE
   	    a, b, t(10:20)
    BEGIN
        ...
    END
    ```
	- Without declaration
    ```java
    BEGIN
        ...
    END
    ```
- Comments
    ```java
    [ OK ]
  
    [ Also
    ...
    OK ]
  
    [ so [ wrong ]  ]
    ```
* Variables
    * Ordinary - [_a-z]+
    ```java
    a   [ ok ]
    _a  [ ok ]
    A1  [ Wrong ]
  ```
    * Array - t(x:y), where x <= y
    ```java
    tab(10:20);  [ declaration of array of size=11 indexed from 10 to 20 ]
    tab(15);     [ Ok ]
    tab(21);     [ Index out of bounds]
    tab(a);      [ if 10 <= a <= 20 Ok ]
    ``` 
    * Constants - only integers >= 0
    ```java
    a := 0;                   [ Ok ]
    a := 1234567890987654321; [ Ok ]
  
    a := -5;                  [ Wrong ]
    a := 123.456;             [ Wrong ]
    ```
  
- IO
    ```java
    READ a;  		[ Read from stdin to variable 'a' ]
    READ 123; 		[ Wrong ]
  
    WRITE a;		[ Write variable 'a' to stdout ]
    WRITE tab(a);	        [ Write array variable to stdout ]
    WRITE 123;		[ Write constant to stdout ]
    ```
- Expressions
    ```java
    a := 10;
    a := b;
    a := 100 * 200;
    a := b * 10;
  
    a := b + c;     [ addition ]
    a := b - c;     [ subtraction ]
    a := b * c;     [ multiplication ]
    a := b / c;     [ division ]
    a := b % c;     [ modulo ]
  
    a := b * c * d; [ Wrong ]
    a := b * c;     [ Only expressions with single operator are allowed ]
    a := a * d;
  
    a / 0 = 0;      [ Division and modulo by zero always equals zero ]
    a % 0 = 0;
    ```

- Conditions
    ```java
    a = b   [ Equals ]
    a != b  [ Not equal ]
    a < b   [ Less ]
    a > b   [ Greater ]
    a <= b  [ Less equal ]
    a >= b  [ Greater equal ]
    ```

- If
    ```java
    [ If ]
    IF a > b [ condition ] THEN 
        ...
    ENDIF
  
    [ If - Else ]
    IF a > b THEN 
        ...
    ELSE
        ...
    ENDIF
    ```  
- Loops
    - While
        ```java
        [ While ]
        WHILE a > b [ condition ] DO
            ...
        ENDWHILE
      
        [ Do - while with negated condition ]
        REPEAT 
            ...
        UNTIL a > b;
        ```  
    - For
        ```java
        [ 'i' is local, not declared variable ]
        [ range (a - b or b - a) is calculated at the begining of loop and cannot be changed ]
        [ for variable range use WHILE ]
        [ i++ ]
        FOR i FROM a TO b DO
            i := c;     [ wrong, local variable cannot be overwritten ]
            a := 2 * a; [ range stays the same ] 
            ...
        ENDFOR
      
        [ i-- ]
        FOR i FROM a DOWNTO b DO
            ...
        ENDFOR
        ```    