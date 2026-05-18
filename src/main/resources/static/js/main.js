// ===== RUN ON PAGE LOAD =====
document.addEventListener('DOMContentLoaded', function () {
    toggleOptions();
    setupMarksCalculator();
});

// ===== MCQ OPTIONS TOGGLE =====
function toggleOptions() {
    var typeSelect = document.getElementById('questionType');
    if (!typeSelect) return;
    var mcqDiv = document.getElementById('mcqOptions');
    if (!mcqDiv) return;
    mcqDiv.style.display =
        (typeSelect.value === 'MCQ') ? 'block' : 'none';
}

// ===== MARKS CALCULATOR =====
// works on both create and edit exam pages
function setupMarksCalculator() {
    var mcqCount = document.getElementById('mcqCount');
    var tfCount  = document.getElementById('tfCount');
    var saCount  = document.getElementById('saCount');
    var mcqMarks = document.getElementById('mcqMarks');
    var tfMarks  = document.getElementById('tfMarks');
    var saMarks  = document.getElementById('saMarks');

    // bail if this page doesn't have the calculator
    if (!mcqCount || !tfCount || !saCount) return;

    var mcqSub   = document.getElementById('mcqSubtotal');
    var tfSub    = document.getElementById('tfSubtotal');
    var saSub    = document.getElementById('saSubtotal');
    var grandEl  = document.getElementById('grandTotal');
    var hidden   = document.getElementById('totalMarksHidden');

    function round2(n) {
        // round to 2 decimal places avoiding floating point drift
        return Math.round(n * 100) / 100;
    }

    function recalc() {
        var mc = parseFloat(mcqCount.value) || 0;
        var tf = parseFloat(tfCount.value)  || 0;
        var sa = parseFloat(saCount.value)  || 0;
        var mm = parseFloat(mcqMarks ? mcqMarks.value : 1) || 0;
        var tm = parseFloat(tfMarks  ? tfMarks.value  : 1) || 0;
        var sm = parseFloat(saMarks  ? saMarks.value  : 2) || 0;

        var mcqTotal = round2(mc * mm);
        var tfTotal  = round2(tf * tm);
        var saTotal  = round2(sa * sm);
        var grand    = round2(mcqTotal + tfTotal + saTotal);

        if (mcqSub)  mcqSub.textContent  = mcqTotal;
        if (tfSub)   tfSub.textContent   = tfTotal;
        if (saSub)   saSub.textContent   = saTotal;
        if (grandEl) grandEl.textContent = grand;
        if (hidden)  hidden.value        = grand;
    }

    // attach listeners
    [mcqCount, tfCount, saCount, mcqMarks, tfMarks, saMarks]
        .forEach(function (el) {
            if (el) {
                el.addEventListener('input',  recalc);
                el.addEventListener('change', recalc);
            }
        });

    // run immediately so existing values show on edit page
    recalc();
}

// ===== EXAM TIMER =====
function startTimer(durationMins) {
    var timerEl = document.getElementById('exam-timer');
    if (!timerEl) return;

    var totalSeconds = durationMins * 60;

    var interval = setInterval(function () {
        var mins = Math.floor(totalSeconds / 60);
        var secs = totalSeconds % 60;
        timerEl.textContent =
            mins + ':' + (secs < 10 ? '0' : '') + secs;

        if (totalSeconds <= 300) {
            timerEl.style.color = 'red';
        }

        if (totalSeconds <= 0) {
            clearInterval(interval);
            var form = document.getElementById('exam-form');
            if (form) form.submit();
        }

        totalSeconds--;
    }, 1000);
}

