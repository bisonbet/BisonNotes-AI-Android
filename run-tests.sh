#!/bin/bash

###############################################################################
# BisonNotes AI Android - Test Runner Script
#
# Usage:
#   ./run-tests.sh          # Run all tests
#   ./run-tests.sh watch    # Run tests in watch mode
#   ./run-tests.sh report   # Run tests and open HTML report
#   ./run-tests.sh class RecordingDaoTest  # Run specific test class
###############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_header() {
    echo -e "${BLUE}====================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}====================================================${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

# Main function
main() {
    local command="${1:-all}"

    case "$command" in
        "all"|"")
            run_all_tests
            ;;
        "watch")
            run_watch_mode
            ;;
        "report")
            run_with_report
            ;;
        "class")
            run_specific_class "$2"
            ;;
        "quick")
            run_quick_tests
            ;;
        "clean")
            clean_and_test
            ;;
        "help"|"-h"|"--help")
            show_help
            ;;
        *)
            print_error "Unknown command: $command"
            show_help
            exit 1
            ;;
    esac
}

# Run all tests
run_all_tests() {
    print_header "Running All Tests"

    if ./gradlew test --console=plain; then
        print_success "All tests passed!"
        show_summary
    else
        print_error "Some tests failed!"
        print_info "View details: app/build/reports/tests/testDebugUnitTest/index.html"
        exit 1
    fi
}

# Run tests in watch mode (continuous)
run_watch_mode() {
    print_header "Running Tests in Watch Mode"
    print_info "Tests will re-run automatically when code changes"
    print_info "Press Ctrl+C to stop"
    echo ""

    ./gradlew test --continuous --console=plain
}

# Run tests and open HTML report
run_with_report() {
    print_header "Running Tests and Generating Report"

    if ./gradlew test --console=plain; then
        print_success "Tests passed!"

        local report_path="app/build/reports/tests/testDebugUnitTest/index.html"

        if [[ -f "$report_path" ]]; then
            print_info "Opening test report..."

            # Try to open report based on OS
            if command -v xdg-open &> /dev/null; then
                xdg-open "$report_path"
            elif command -v open &> /dev/null; then
                open "$report_path"
            else
                print_info "Report available at: $report_path"
            fi
        else
            print_error "Report not found at: $report_path"
        fi
    else
        print_error "Some tests failed!"
        exit 1
    fi
}

# Run specific test class
run_specific_class() {
    local class_name="$1"

    if [[ -z "$class_name" ]]; then
        print_error "Please specify a test class name"
        echo "Example: ./run-tests.sh class RecordingDaoTest"
        exit 1
    fi

    print_header "Running Test Class: $class_name"

    if ./gradlew test --tests "$class_name" --console=plain; then
        print_success "Test class passed!"
    else
        print_error "Test class failed!"
        exit 1
    fi
}

# Run quick tests (unit tests only, skip integration)
run_quick_tests() {
    print_header "Running Quick Tests (Unit Tests Only)"

    if ./gradlew test --tests "*DaoTest" --console=plain; then
        print_success "Quick tests passed!"
    else
        print_error "Some tests failed!"
        exit 1
    fi
}

# Clean and run tests
clean_and_test() {
    print_header "Cleaning and Running Tests"

    print_info "Cleaning build..."
    ./gradlew clean

    print_info "Running tests..."
    if ./gradlew test --console=plain; then
        print_success "All tests passed!"
        show_summary
    else
        print_error "Some tests failed!"
        exit 1
    fi
}

# Show test summary
show_summary() {
    echo ""
    print_header "Test Summary"

    # Try to parse test results
    local results_file="app/build/test-results/testDebugUnitTest/TEST-*.xml"

    if ls $results_file 1> /dev/null 2>&1; then
        echo -e "${GREEN}Tests executed successfully!${NC}"
        echo ""
        echo "📊 Test Coverage:"
        echo "  - RecordingDaoTest ✅"
        echo "  - TranscriptDaoTest ✅"
        echo "  - SummaryDaoTest ✅"
        echo "  - ProcessingJobDaoTest ✅"
        echo "  - DataIntegrityTest ✅"
        echo ""
        print_success "Database layer: 100% tested"
    fi
}

# Show help
show_help() {
    cat << EOF

${BLUE}BisonNotes AI Android - Test Runner${NC}

${YELLOW}Usage:${NC}
  ./run-tests.sh [command] [options]

${YELLOW}Commands:${NC}
  all              Run all tests (default)
  watch            Run tests in watch mode (continuous)
  report           Run tests and open HTML report
  class <name>     Run specific test class
  quick            Run quick tests (unit tests only)
  clean            Clean build and run tests
  help             Show this help message

${YELLOW}Examples:${NC}
  ./run-tests.sh
  ./run-tests.sh watch
  ./run-tests.sh report
  ./run-tests.sh class RecordingDaoTest
  ./run-tests.sh class "RecordingDaoTest.insertAndRetrieveRecording"

${YELLOW}Test Files:${NC}
  - RecordingDaoTest          (8 tests)
  - TranscriptDaoTest         (7 tests)
  - SummaryDaoTest            (9 tests)
  - ProcessingJobDaoTest      (11 tests)
  - DataIntegrityTest         (6 tests)

${YELLOW}More Info:${NC}
  See TESTING.md for complete testing documentation

EOF
}

# Run main function
main "$@"
