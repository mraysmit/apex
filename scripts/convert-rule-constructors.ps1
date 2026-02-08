# Script to convert 4-param Rule constructor calls to RuleBuilder
# Handles both single-line and multi-line patterns

$baseDir = "c:\Users\mraysmit\dev\idea-projects\apex-rules-engine\apex-demo\src"

$files = Get-ChildItem -Recurse -Path $baseDir -Filter "*.java" | 
    Select-String "new Rule\(" | 
    Select-Object -ExpandProperty Path -Unique

$importLine = "import dev.mars.apex.core.engine.config.RuleBuilder;"
$totalReplaced = 0

foreach ($filePath in $files) {
    Write-Host "`n=== Processing: $($filePath | Split-Path -Leaf) ==="
    $content = [System.IO.File]::ReadAllText($filePath)
    $originalContent = $content
    
    # Add import if not already present
    if ($content -notmatch "import dev\.mars\.apex\.core\.engine\.config\.RuleBuilder;") {
        # Insert after the last "import dev.mars.apex" line
        $content = $content -replace "(import dev\.mars\.apex\.[^;]+;`n)(?!import dev\.mars\.apex)", "`$1$importLine`n"
        if ($content -eq $originalContent) {
            # Fallback: insert before the first non-import line after package
            $content = $content -replace "(import [^;]+;`n)(?!import )", "`$1$importLine`n"
        }
    }
    
    # Pattern 1: Single-line new Rule("str", "str", "str", "str") with simple quoted args
    # Handles: new Rule("name", "cond", "msg", "sev")
    $pattern1 = 'new Rule\(\s*"([^"]*)",\s*"([^"]*)",\s*"([^"]*)",\s*"([^"]*)"\s*\)'
    $count1 = ([regex]::Matches($content, $pattern1)).Count
    $content = [regex]::Replace($content, $pattern1, {
        param($m)
        $name = $m.Groups[1].Value
        $cond = $m.Groups[2].Value
        $msg = $m.Groups[3].Value
        $sev = $m.Groups[4].Value
        "new RuleBuilder().withName(`"$name`").withCondition(`"$cond`").withMessage(`"$msg`").withSeverity(`"$sev`").build()"
    })
    
    # Pattern 2: Single-line with SeverityConstants as 4th arg
    # Handles: new Rule("name", "cond", "msg", SeverityConstants.INFO)
    $pattern2 = 'new Rule\(\s*"([^"]*)",\s*"([^"]*)",\s*"([^"]*)",\s*(SeverityConstants\.\w+)\s*\)'
    $count2 = ([regex]::Matches($content, $pattern2)).Count
    $content = [regex]::Replace($content, $pattern2, {
        param($m)
        $name = $m.Groups[1].Value
        $cond = $m.Groups[2].Value
        $msg = $m.Groups[3].Value
        $sev = $m.Groups[4].Value
        "new RuleBuilder().withName(`"$name`").withCondition(`"$cond`").withMessage(`"$msg`").withSeverity($sev).build()"
    })
    
    # Pattern 3: Multi-line new Rule(\n    "str",\n    "str",\n    "str",\n    "str"\n)
    # Uses dotall flag (?s) to match across lines
    $pattern3 = '(?s)new Rule\(\s*"([^"]*)",\s*"([^"]*)",\s*"([^"]*)",\s*"([^"]*)"\s*\)'
    $count3 = ([regex]::Matches($content, $pattern3)).Count - $count1  # Subtract already-matched single-line
    $content = [regex]::Replace($content, $pattern3, {
        param($m)
        $name = $m.Groups[1].Value
        $cond = $m.Groups[2].Value
        $msg = $m.Groups[3].Value
        $sev = $m.Groups[4].Value
        "new RuleBuilder().withName(`"$name`").withCondition(`"$cond`").withMessage(`"$msg`").withSeverity(`"$sev`").build()"
    })
    
    # Pattern 4: Multi-line with SeverityConstants as 4th arg
    $pattern4 = '(?s)new Rule\(\s*"([^"]*)",\s*"([^"]*)",\s*"([^"]*)",\s*(SeverityConstants\.\w+)\s*\)'
    $count4 = ([regex]::Matches($content, $pattern4)).Count - $count2
    $content = [regex]::Replace($content, $pattern4, {
        param($m)
        $name = $m.Groups[1].Value
        $cond = $m.Groups[2].Value
        $msg = $m.Groups[3].Value
        $sev = $m.Groups[4].Value
        "new RuleBuilder().withName(`"$name`").withCondition(`"$cond`").withMessage(`"$msg`").withSeverity($sev).build()"
    })
    
    # Pattern 5: Single-line with string concatenation in message (3rd arg)
    # Handles: new Rule("name", "cond", "text" + var, "sev")
    $pattern5 = 'new Rule\(\s*"([^"]*)",\s*"([^"]*)",\s*("([^"]*)" \+ [^,]+),\s*"([^"]*)"\s*\)'
    $count5 = ([regex]::Matches($content, $pattern5)).Count
    $content = [regex]::Replace($content, $pattern5, {
        param($m)
        $name = $m.Groups[1].Value
        $cond = $m.Groups[2].Value
        $msgExpr = $m.Groups[3].Value
        $sev = $m.Groups[5].Value
        "new RuleBuilder().withName(`"$name`").withCondition(`"$cond`").withMessage($msgExpr).withSeverity(`"$sev`").build()"
    })
    
    $replaced = $count1 + $count2 + $count3 + $count4 + $count5
    $totalReplaced += $replaced
    
    if ($content -ne $originalContent) {
        [System.IO.File]::WriteAllText($filePath, $content)
        Write-Host "  Replaced ~$replaced calls"
    } else {
        Write-Host "  No changes"
    }
    
    # Check remaining "new Rule(" calls
    $remaining = ([regex]::Matches($content, 'new Rule\(')).Count
    if ($remaining -gt 0) {
        Write-Host "  WARNING: $remaining 'new Rule(' calls still remain (may be 16-param calls)"
    }
}

Write-Host "`n=== Total replaced: $totalReplaced ==="
