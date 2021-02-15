# Compiler - Alpha

Compiles to simplified assembly used by virtual machine with 6 registers a-f.

## Grammar
- Program structure
    - With declaration
    ```c
    DECLARE
   	    a, b, t(10:20)
    BEGIN
        ...
    END
    ```
	- Without declaration
    ```c
    BEGIN
        ...
    END
    ```
- Comments
    ```c
    [ OK ]
  
    [ Also
    ...
    OK ]
  
    [ so [ wrong ]  ]
    ```
* Variables
    * Ordinary - [_a-z]+
    ```c
    a   [ ok ]
    _a  [ ok ]
    A1  [ Wrong ]
  ```
    * Array - t(x:y), where x <= y
    ```c
    tab(10:20);  [ declaration of array of size=11 indexed from 10 to 20 ]
    tab(15);     [ Ok ]
    tab(21);     [ Index out of bounds]
    tab(a);      [ if 10 <= a <= 20 Ok ]
    ``` 
    * Constants - only integers >= 0
    ```c
    a := 0;                   [ Ok ]
    a := 1234567890987654321; [ Ok ]
    a := 0;                   [ Ok ]
    a := 1234567890987654321; [ Ok ]
    ```
  
- IO
    ```c
    READ a;  		[ Read from stdin to variable 'a' ]
    READ 123; 		[ Wrong ]
  
    WRITE a;		[ Write variable 'a' to stdout ]
    WRITE tab(a);	        [ Write array variable to stdout ]
    WRITE 123;		[ Write constant to stdout ]
    ```
- Expressions
    ```c
    a := 10;
    a := b;
    a := 100 * 200;
    a := b * 10;
  
    a := b + c;     [ add ]
    a := b - c;     [ subtract ]
    a := b * c;     [ multiply ]
    a := b / c;     [ divide ]
    a := b % c;     [ modulo ]
  
    a := b * c * d; [ Wrong ]
    a := b * c;     [ Only expressions with single operator are allowed ]
    a := a * d;
  
    a / 0 = 0;      [ Division and modulo by zero always equals zero ]
    a % 0 = 0;
    ```

- Conditions
    ```c
    a = b   [ Equals ]
    a != b  [ Not equal ]
    a < b   [ Less ]
    a > b   [ Greater ]
    a <= b  [ Less equal ]
    a >= b  [ Greater equal ]
    ```

- If
    ```c
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
        ```c
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
        ```c
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