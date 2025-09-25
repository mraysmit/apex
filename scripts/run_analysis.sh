#!/bin/bash
# APEX Demo YAML Analysis Runner
# Convenience script for running the YAML analysis with common options

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APEX_ROOT="$(dirname "$SCRIPT_DIR")"

echo -e "${BLUE}🔍 APEX Demo YAML Analysis Runner${NC}"
echo -e "${BLUE}=================================${NC}"

# Check if we're in the right directory
if [ ! -d "$APEX_ROOT/apex-demo" ]; then
    echo -e "${RED}X Error: apex-demo directory not found${NC}"
    echo -e "${YELLOW}Please run this script from the APEX root directory${NC}"
    exit 1
fi

# Check Python installation
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}X Error: Python 3 is required but not installed${NC}"
    exit 1
fi

# Check if virtual environment should be created
if [ ! -d "$SCRIPT_DIR/venv" ]; then
    echo -e "${YELLOW}📦 Creating virtual environment...${NC}"
    python3 -m venv "$SCRIPT_DIR/venv"
fi

# Activate virtual environment
echo -e "${YELLOW}🔧 Activating virtual environment...${NC}"
source "$SCRIPT_DIR/venv/bin/activate" 2>/dev/null || source "$SCRIPT_DIR/venv/Scripts/activate" 2>/dev/null

# Install dependencies
echo -e "${YELLOW}📥 Installing dependencies...${NC}"
pip install -q -r "$SCRIPT_DIR/requirements.txt"

# Set default output files
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
OUTPUT_DIR="$APEX_ROOT/reports"
MARKDOWN_REPORT="$OUTPUT_DIR/demo_yaml_analysis_$TIMESTAMP.md"
JSON_REPORT="$OUTPUT_DIR/demo_yaml_analysis_$TIMESTAMP.json"

# Create reports directory
mkdir -p "$OUTPUT_DIR"

# Parse command line arguments
VERBOSE=""
CUSTOM_OUTPUT=""
CUSTOM_JSON=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--verbose)
            VERBOSE="--verbose"
            shift
            ;;
        -o|--output)
            CUSTOM_OUTPUT="$2"
            shift 2
            ;;
        -j|--json)
            CUSTOM_JSON="$2"
            shift 2
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -v, --verbose     Enable verbose output"
            echo "  -o, --output FILE Custom markdown output file"
            echo "  -j, --json FILE   Custom JSON output file"
            echo "  -h, --help        Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                          # Run with default settings"
            echo "  $0 --verbose                # Run with verbose output"
            echo "  $0 -o my_report.md          # Custom markdown output"
            echo "  $0 -o report.md -j data.json # Custom outputs"
            exit 0
            ;;
        *)
            echo -e "${RED}X Unknown option: $1${NC}"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Use custom outputs if provided
if [ -n "$CUSTOM_OUTPUT" ]; then
    MARKDOWN_REPORT="$CUSTOM_OUTPUT"
fi

if [ -n "$CUSTOM_JSON" ]; then
    JSON_REPORT="$CUSTOM_JSON"
fi

# Run the analysis
echo -e "${GREEN}🚀 Running APEX Demo YAML Analysis...${NC}"
echo -e "${BLUE}📁 APEX Root: $APEX_ROOT${NC}"
echo -e "${BLUE}📝 Markdown Report: $MARKDOWN_REPORT${NC}"
echo -e "${BLUE}📊 JSON Report: $JSON_REPORT${NC}"
echo ""

python3 "$SCRIPT_DIR/analyze_demo_yaml_files.py" \
    --apex-root "$APEX_ROOT" \
    --output "$MARKDOWN_REPORT" \
    --json "$JSON_REPORT" \
    $VERBOSE

# Check if analysis was successful
if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✅ Analysis completed successfully!${NC}"
    echo ""
    echo -e "${BLUE}📋 Generated Reports:${NC}"
    echo -e "  📝 Markdown: $MARKDOWN_REPORT"
    echo -e "  📊 JSON:     $JSON_REPORT"
    echo ""
    
    # Show file sizes
    if [ -f "$MARKDOWN_REPORT" ]; then
        MD_SIZE=$(du -h "$MARKDOWN_REPORT" | cut -f1)
        echo -e "  📏 Markdown size: $MD_SIZE"
    fi
    
    if [ -f "$JSON_REPORT" ]; then
        JSON_SIZE=$(du -h "$JSON_REPORT" | cut -f1)
        echo -e "  📏 JSON size: $JSON_SIZE"
    fi
    
    echo ""
    echo -e "${YELLOW}💡 Next steps:${NC}"
    echo -e "  • Review the markdown report for detailed analysis"
    echo -e "  • Use the JSON report for programmatic processing"
    echo -e "  • Check recommendations section for improvements"
    
    # Offer to open the report
    if command -v open &> /dev/null; then
        echo ""
        read -p "📖 Open markdown report now? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            open "$MARKDOWN_REPORT"
        fi
    elif command -v xdg-open &> /dev/null; then
        echo ""
        read -p "📖 Open markdown report now? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            xdg-open "$MARKDOWN_REPORT"
        fi
    fi
    
else
    echo -e "${RED}X Analysis failed!${NC}"
    echo -e "${YELLOW}Check the error messages above for details${NC}"
    exit 1
fi

# Deactivate virtual environment
deactivate 2>/dev/null || true

echo -e "${GREEN}🎉 All done!${NC}"
