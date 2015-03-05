Feature: Scans application for packages and classes
    As a CS teacher
    I want to test students' programming assignments
    In order to find out if classes have been correctly created

    Background:
        Given the maximum grade is 100
        Given the main class is 'Pratica91'
        Given I set the script timeout to 3000
        Given I evaluate 'import utfpr.ct.dainf.grader.*;'
        Given I evaluate 'import utfpr.ct.dainf.if62c.pratica.*'
    
    Scenario: Verifica se o SO é exibido
        Given I report 'Iniciando avaliação...'
        Given I report 'Avaliando item 4...'
        Given I set output to <mainOut>
        Given I evaluate 'Pratica91.main(new String[0])'
        Given I evaluate 'totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024'
        Given I evaluate 'freeMem = Runtime.getRuntime().freeMemory() / 1024 / 1024'
        Given I evaluate 'maxMem = Runtime.getRuntime().maxMemory() / 1024 / 1024'
        Given I set output to <default>
        And <mainOut> matches regex '(?i)(?s).*(?:linux|windows).*'
        Then award 20 points

    Scenario: Verifica se o número de processadores é exibido
        Given I evaluate 'regex = "(?i)(?s).*" + String.valueOf(Runtime.getRuntime().availableProcessors()) + ".*"'
        And <mainOut> matches regex <regex>
        Then award 20 points

    Scenario: Verifica se a memória total é exibida
        Given I evaluate 'regex = "(?i)(?s).*" + String.valueOf(totalMem) + ".*"'
        And <mainOut> matches regex <regex>
        Then award 20 points

    Scenario: Verifica se a memória livre é exibida
        Given I evaluate 'regex = "(?i)(?s).*" + String.valueOf(freeMem) + ".*"'
        And <mainOut> matches regex <regex>
        Then award 20 points

    Scenario: Verifica se a memória máxima é exibida
        Given I evaluate 'regex = "(?i)(?s).*" + String.valueOf(maxMem) + ".*"'
        And <mainOut> matches regex <regex>
        Then award 20 points

    Scenario: Report final grade.
        Given I set output to <default>
        Given I report grade formatted as 'FINAL GRADE: %.1f'
