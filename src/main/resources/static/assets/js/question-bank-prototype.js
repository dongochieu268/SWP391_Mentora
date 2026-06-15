(function () {
  "use strict";

  var bankQuestions = [
    {
      content: "Trong Java, từ khóa nào được dùng để kế thừa một lớp?",
      type: "MULTIPLE_CHOICE",
      difficulty: "EASY",
      score: 1,
      options: ["extends", "implements", "inherits", "super"],
      correct: "extends"
    },
    {
      content: "HTTP là giao thức không lưu trạng thái (stateless).",
      type: "TRUE_FALSE",
      difficulty: "EASY",
      score: 1,
      options: ["Đúng", "Sai"],
      correct: "Đúng"
    },
    {
      content: "Annotation nào dùng để đánh dấu một lớp là Spring MVC Controller?",
      type: "MULTIPLE_CHOICE",
      difficulty: "MEDIUM",
      score: 1,
      options: ["@Controller", "@ComponentScan", "@Bean", "@Configuration"],
      correct: "@Controller"
    },
    {
      content: "Đâu là đặc điểm của transaction ACID?",
      type: "MULTIPLE_CHOICE",
      difficulty: "MEDIUM",
      score: 1,
      options: ["Atomicity", "Concurrency", "Inheritance", "Serialization"],
      correct: "Atomicity"
    },
    {
      content: "Phương thức HTTP nào thường được sử dụng để cập nhật một phần tài nguyên?",
      type: "MULTIPLE_CHOICE",
      difficulty: "HARD",
      score: 1,
      options: ["PATCH", "GET", "TRACE", "OPTIONS"],
      correct: "PATCH"
    }
  ];

  function shuffle(items) {
    var copy = items.slice();
    for (var i = copy.length - 1; i > 0; i--) {
      var j = Math.floor(Math.random() * (i + 1));
      var current = copy[i];
      copy[i] = copy[j];
      copy[j] = current;
    }
    return copy;
  }

  function escapeHtml(value) {
    return String(value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  function showToast(message) {
    var toast = document.getElementById("prototypeToast");
    if (!toast) return;
    toast.querySelector(".toast-body").textContent = message;
    bootstrap.Toast.getOrCreateInstance(toast).show();
  }

  function bindModeSwitching() {
    var buttons = document.querySelectorAll("[data-editor-mode]");
    buttons.forEach(function (button) {
      button.addEventListener("click", function () {
        buttons.forEach(function (item) { item.classList.remove("active"); });
        document.querySelectorAll(".prototype-mode-panel").forEach(function (panel) {
          panel.classList.remove("active");
        });
        button.classList.add("active");
        var target = document.getElementById(button.dataset.editorMode);
        if (target) target.classList.add("active");
      });
    });
  }

  function bindQuestionType() {
    document.querySelectorAll("[data-question-type]").forEach(function (select) {
      select.addEventListener("change", function () {
        var scope = select.closest("form") || select.closest(".modal-content");
        if (!scope) return;
        var answerList = scope.querySelector("[data-answer-list]");
        if (!answerList) return;
        if (select.value === "TRUE_FALSE") {
          answerList.innerHTML =
            '<div class="prototype-answer-row">' +
              '<input class="form-check-input" type="radio" name="correctAnswer" checked>' +
              '<input class="form-control" value="Đúng" readonly>' +
              '<button class="prototype-icon-button invisible" type="button" tabindex="-1"><i class="ti ti-x"></i></button>' +
            '</div>' +
            '<div class="prototype-answer-row">' +
              '<input class="form-check-input" type="radio" name="correctAnswer">' +
              '<input class="form-control" value="Sai" readonly>' +
              '<button class="prototype-icon-button invisible" type="button" tabindex="-1"><i class="ti ti-x"></i></button>' +
            '</div>';
        }
      });
    });
  }

  function bindAddAnswer() {
    document.querySelectorAll("[data-add-answer]").forEach(function (button) {
      button.addEventListener("click", function () {
        var scope = button.closest("form") || button.closest(".modal-content");
        var list = scope ? scope.querySelector("[data-answer-list]") : null;
        if (!list) return;
        var index = list.querySelectorAll(".prototype-answer-row").length + 1;
        var row = document.createElement("div");
        row.className = "prototype-answer-row";
        row.innerHTML =
          '<input class="form-check-input" type="radio" name="correctAnswer">' +
          '<input class="form-control" placeholder="Đáp án ' + index + '">' +
          '<button class="prototype-icon-button" type="button" data-remove-answer aria-label="Xóa đáp án"><i class="ti ti-x"></i></button>';
        list.appendChild(row);
      });
    });

    document.addEventListener("click", function (event) {
      var remove = event.target.closest("[data-remove-answer]");
      if (!remove) return;
      var list = remove.closest("[data-answer-list]");
      if (list && list.querySelectorAll(".prototype-answer-row").length > 2) {
        remove.closest(".prototype-answer-row").remove();
      } else {
        showToast("Mỗi câu hỏi cần ít nhất 2 đáp án.");
      }
    });
  }

  function bindBankSelection() {
    var checks = document.querySelectorAll("[data-bank-question]");
    var counter = document.querySelector("[data-bank-selected-count]");
    var button = document.querySelector("[data-add-selected-bank]");

    function refresh() {
      var count = document.querySelectorAll("[data-bank-question]:checked").length;
      if (counter) counter.textContent = count;
      if (button) button.disabled = count === 0;
    }

    checks.forEach(function (check) { check.addEventListener("change", refresh); });
    refresh();

    if (button) {
      button.addEventListener("click", function () {
        var selected = Array.from(document.querySelectorAll("[data-bank-question]:checked"));
        selected.forEach(function (check) {
          var question = bankQuestions[Number(check.value)];
          if (question) appendQuestionCard(question, "MANUAL_IMPORT");
          check.checked = false;
        });
        refresh();
        showToast("Đã thêm " + selected.length + " câu hỏi từ Question Bank.");
      });
    }
  }

  function renderRandomPreview() {
    var preview = document.querySelector("[data-random-preview]");
    var quantity = document.querySelector("[data-random-quantity]");
    if (!preview || !quantity) return;

    var count = Math.min(Math.max(Number(quantity.value) || 1, 1), bankQuestions.length);
    var selected = shuffle(bankQuestions).slice(0, count).map(function (question) {
      return Object.assign({}, question, { options: shuffle(question.options) });
    });
    preview.dataset.questions = JSON.stringify(selected);
    preview.innerHTML =
      '<div class="d-flex align-items-center justify-content-between mb-3">' +
        '<strong>' + selected.length + ' câu đã được random</strong>' +
        '<span class="prototype-chip"><i class="ti ti-arrows-shuffle"></i> Đáp án đã đảo</span>' +
      '</div>' +
      selected.map(function (question, index) {
        return '<div class="prototype-preview-item">' +
          '<div class="fw-semibold">' + (index + 1) + '. ' + escapeHtml(question.content) + '</div>' +
          '<ol class="prototype-preview-options">' +
            question.options.map(function (option) { return '<li>' + escapeHtml(option) + '</li>'; }).join("") +
          '</ol>' +
        '</div>';
      }).join("");
    preview.classList.add("show");
  }

  function bindRandom() {
    var previewButton = document.querySelector("[data-preview-random]");
    var rerollButton = document.querySelector("[data-reroll-random]");
    var confirmButton = document.querySelector("[data-confirm-random]");
    if (previewButton) previewButton.addEventListener("click", renderRandomPreview);
    if (rerollButton) rerollButton.addEventListener("click", renderRandomPreview);
    if (confirmButton) {
      confirmButton.addEventListener("click", function () {
        var preview = document.querySelector("[data-random-preview]");
        var selected = preview && preview.dataset.questions ? JSON.parse(preview.dataset.questions) : [];
        selected.forEach(function (question) { appendQuestionCard(question, "RANDOM_IMPORT"); });
        if (preview) {
          preview.classList.remove("show");
          preview.innerHTML = "";
          preview.dataset.questions = "";
        }
        showToast("Đã thêm câu hỏi và lưu thứ tự đáp án vừa random.");
      });
    }
  }

  function sourceLabel(source) {
    if (source === "MANUAL_IMPORT") return "Từ Question Bank";
    if (source === "RANDOM_IMPORT") return "Random từ Question Bank";
    return "Tạo thủ công";
  }

  function appendQuestionCard(question, source) {
    var list = document.querySelector("[data-test-question-list]");
    if (!list) return;
    var id = "mock-" + Date.now() + "-" + Math.floor(Math.random() * 1000);
    var card = document.createElement("article");
    card.className = "prototype-test-question";
    card.dataset.questionSource = source;
    card.innerHTML =
      '<div class="prototype-question-top">' +
        (source === "CUSTOM"
          ? '<input class="form-check-input" type="checkbox" data-save-bank-question aria-label="Chọn lưu vào Question Bank">'
          : '<span></span>') +
        '<div>' +
          '<div class="prototype-question-title">' + escapeHtml(question.content) + '</div>' +
          '<div class="prototype-question-meta">' +
            '<span>' + sourceLabel(source) + '</span>' +
            '<span>' + escapeHtml(question.difficulty) + '</span>' +
            '<span>' + escapeHtml(question.score) + ' điểm</span>' +
          '</div>' +
          '<div class="mt-2" data-bank-state>' +
            (source === "CUSTOM" ? '<span class="prototype-chip">Chưa lưu vào Question Bank</span>' : '<span class="prototype-chip"><i class="ti ti-database"></i> Bản sao độc lập</span>') +
          '</div>' +
        '</div>' +
        '<div class="prototype-question-actions">' +
          '<button class="prototype-icon-button" type="button" title="Sửa câu hỏi" data-edit-question><i class="ti ti-pencil"></i></button>' +
          '<button class="prototype-icon-button" type="button" title="Xóa câu hỏi" data-delete-question><i class="ti ti-trash"></i></button>' +
        '</div>' +
      '</div>' +
      '<div class="prototype-options-preview">' +
        question.options.map(function (option, index) {
          var correct = option === question.correct;
          return '<div class="prototype-option-preview' + (correct ? ' is-correct' : '') + '">' +
            '<span class="prototype-option-letter">' + String.fromCharCode(65 + index) + '</span>' +
            '<span>' + escapeHtml(option) + '</span>' +
          '</div>';
        }).join("") +
      '</div>';
    card.id = id;
    list.prepend(card);
    refreshQuestionCount();
  }

  function bindManualQuestion() {
    var form = document.querySelector("[data-manual-question-form]");
    if (!form) return;
    form.addEventListener("submit", function (event) {
      event.preventDefault();
      var content = form.querySelector("[name=manualContent]").value.trim();
      if (!content) return;
      var options = Array.from(form.querySelectorAll("[data-answer-list] .form-control"))
        .map(function (input) { return input.value.trim(); })
        .filter(Boolean);
      var checked = form.querySelector("[data-answer-list] .form-check-input:checked");
      var correctRow = checked ? checked.closest(".prototype-answer-row") : null;
      var correct = correctRow ? correctRow.querySelector(".form-control").value.trim() : options[0];
      appendQuestionCard({
        content: content,
        type: form.querySelector("[data-question-type]").value,
        difficulty: form.querySelector("[name=manualDifficulty]").value,
        score: Number(form.querySelector("[name=manualScore]").value) || 1,
        options: options,
        correct: correct
      }, "CUSTOM");
      form.reset();
      showToast("Đã thêm câu hỏi thủ công vào bài test.");
    });
  }

  function refreshQuestionCount() {
    var count = document.querySelectorAll("[data-test-question-list] .prototype-test-question").length;
    document.querySelectorAll("[data-test-question-count]").forEach(function (item) {
      item.textContent = count;
    });
  }

  function bindQuestionActions() {
    document.addEventListener("click", function (event) {
      var deleteButton = event.target.closest("[data-delete-question]");
      if (deleteButton) {
        deleteButton.closest(".prototype-test-question").remove();
        refreshQuestionCount();
        refreshSelectionBar();
        showToast("Đã xóa câu hỏi khỏi bản prototype.");
      }

      var editButton = event.target.closest("[data-edit-question]");
      if (editButton) {
        var card = editButton.closest(".prototype-test-question");
        var modalElement = document.getElementById("editQuestionModal");
        var form = document.querySelector("[data-edit-question-form]");
        if (!card || !modalElement || !form) return;
        if (!card.id) card.id = "edit-" + Date.now() + "-" + Math.floor(Math.random() * 1000);
        form.dataset.cardId = card.id;
        form.querySelector("[name=editContent]").value =
          card.querySelector(".prototype-question-title").textContent.trim();
        var optionInputs = form.querySelectorAll("[data-edit-answer-list] .form-control");
        var currentOptions = card.querySelectorAll(".prototype-option-preview");
        optionInputs.forEach(function (input, index) {
          input.value = currentOptions[index] ? currentOptions[index].textContent.trim().replace(/^[A-Z]\s*/, "") : "";
        });
        bootstrap.Modal.getOrCreateInstance(modalElement).show();
      }
    });
  }

  function bindEditQuestion() {
    var form = document.querySelector("[data-edit-question-form]");
    if (!form) return;
    form.addEventListener("submit", function (event) {
      event.preventDefault();
      var card = document.getElementById(form.dataset.cardId);
      if (!card) return;
      card.querySelector(".prototype-question-title").textContent =
        form.querySelector("[name=editContent]").value.trim();
      var modal = bootstrap.Modal.getInstance(document.getElementById("editQuestionModal"));
      if (modal) modal.hide();
      showToast("Đã cập nhật bản sao câu hỏi trong bài test.");
    });
  }

  function refreshSelectionBar() {
    var selected = document.querySelectorAll("[data-save-bank-question]:checked");
    var bar = document.querySelector("[data-selection-bar]");
    var count = document.querySelector("[data-selection-count]");
    document.querySelectorAll("[data-save-bank-question]").forEach(function (checkbox) {
      var card = checkbox.closest(".prototype-test-question");
      if (card) card.classList.toggle("is-selected", checkbox.checked);
    });
    if (count) count.textContent = selected.length;
    if (bar) bar.classList.toggle("show", selected.length > 0);
  }

  function bindSaveToBank() {
    document.addEventListener("change", function (event) {
      if (event.target.matches("[data-save-bank-question]")) refreshSelectionBar();
    });

    var saveButton = document.querySelector("[data-save-selected-bank]");
    if (saveButton) {
      saveButton.addEventListener("click", function () {
        var selected = Array.from(document.querySelectorAll("[data-save-bank-question]:checked"));
        selected.forEach(function (checkbox) {
          var card = checkbox.closest(".prototype-test-question");
          checkbox.remove();
          card.classList.remove("is-selected");
          card.querySelector("[data-bank-state]").innerHTML =
            '<span class="prototype-chip is-easy"><i class="ti ti-check"></i> Đã lưu vào Question Bank</span>';
        });
        refreshSelectionBar();
        showToast("Đã lưu " + selected.length + " câu hỏi thủ công vào Question Bank môn PRJ301.");
      });
    }
  }

  function bindModalSubmit() {
    var form = document.querySelector("[data-bank-question-form]");
    if (!form) return;
    form.addEventListener("submit", function (event) {
      event.preventDefault();
      var modal = bootstrap.Modal.getInstance(document.getElementById("questionModal"));
      if (modal) modal.hide();
      showToast("Đã thêm câu hỏi vào Question Bank chung của môn PRJ301.");
    });
  }

  function bindBankFilters() {
    var search = document.querySelector("[data-bank-search]");
    if (!search) return;
    search.addEventListener("input", function () {
      var term = search.value.trim().toLowerCase();
      document.querySelectorAll("[data-bank-row]").forEach(function (row) {
        row.hidden = !row.textContent.toLowerCase().includes(term);
      });
    });
  }

  bindModeSwitching();
  bindQuestionType();
  bindAddAnswer();
  bindBankSelection();
  bindRandom();
  bindManualQuestion();
  bindQuestionActions();
  bindEditQuestion();
  bindSaveToBank();
  bindModalSubmit();
  bindBankFilters();
  refreshQuestionCount();
})();
