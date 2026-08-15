const form = document.getElementById("uploadForm");
const fileInput = document.getElementById("files");

const result = document.getElementById("result");

const resultsSection =
    document.getElementById("resultsSection");

const resultsSummary =
    document.getElementById("resultsSummary");

const resultsTable =
    document.getElementById("resultsTable");

const downloadCsvButton =
    document.getElementById("downloadCsvButton");

downloadCsvButton.addEventListener("click", downloadCsv);

const MAX_FILES = 3;
let currentResults = [];

/*
        Upload
 */
form.addEventListener("submit", async function(event) {
    event.preventDefault();
    downloadCsvButton.classList.add("d-none");
    const files = fileInput.files;

    // -------------------------
    // Validation
    // -------------------------
    hideError();

    if (files.length === 0) {
        showError("Please select at least one PDF file.");
        return;
    }

    if (files.length > MAX_FILES) {
        showError(`You can upload a maximum of ${MAX_FILES} payslips at once.`);
        return;
    }

    for (const file of files) {
        if (file.type !== "application/pdf") {
            showError(`"${file.name}" is not a PDF file.`);
            return;
        }
    }

    // -------------------------
    // Prepare files
    // -------------------------
    const formData = new FormData();
    for (const file of files) {
        formData.append("files", file);
    }

    // -------------------------
    // Processing message
    // -------------------------
    result.innerHTML = `
        <div class="alert alert-info mt-4">
            <strong>Processing payslips...</strong><br>
            Depending on the size and number of files,
            this may take a few minutes.
        </div>
    `;

    resultsSection.classList.add("d-none");

    try {
        const response = await fetch(
            "/api/payslip/upload",
            {
                method: "POST",
                body: formData
            }
        );

        if (!response.ok) {
            const message = await response.text();
            throw new Error(message);
        }

        const data = await response.json();
        console.log("Backend response:", data);
        result.textContent = "";
        currentResults = data; // Needed to save in csv/excel
        // displayResults(data);

        // -------------------------
        // Display results
        // -------------------------
        displayResults(data);
        downloadCsvButton.classList.remove("d-none");
        result.innerHTML = "";
    } catch (error) {
        console.error(error);
        result.innerHTML = `
            <div class="alert alert-danger mt-4">
                Error: ${error.message}
            </div>
        `;
    }
});

/*
        Generating table
 */
function displayResults(data) {
    console.log("Results received:", data);

    // Your backend should return an array
    // of PayslipResult objects.
    const resultsSection = document.getElementById("resultsSection");
    const tableHead = document.getElementById("resultsTableHead");
    const tableBody = document.getElementById("resultsTableBody");

    console.log("resultsSection:", resultsSection);
    console.log("tableHead:", tableHead);
    console.log("tableBody:", tableBody);

    tableHead.innerHTML = "";
    tableBody.innerHTML = "";

    const results = Array.isArray(data)
        ? data
        : data.results;

    if (!results || results.length === 0) {
        resultsSection.classList.add("d-none");
        result.innerHTML = `
            <div class="alert alert-warning mt-4">
                No extraction results were returned.
            </div>
        `;
        return;
    }
    resultsSummary.textContent =`${results.length} payslip(s) processed successfully.`;
    buildResultsTable(results);
    resultsSection.classList.remove("d-none");
}

function buildResultsTable(results) {
    const tableHead = resultsTable.querySelector("thead");
    const tableBody = resultsTable.querySelector("tbody");

    // Clear previous results
    tableHead.innerHTML = "";
    tableBody.innerHTML = "";

    // --------------------------------
    // Create header
    // --------------------------------
    const headerRow = document.createElement("tr");
    const fieldHeader = document.createElement("th");
    fieldHeader.textContent = "Extracted Field";
    headerRow.appendChild(fieldHeader);

    results.forEach((payslip, index) => {
        const th =
            document.createElement("th");
        th.textContent =
            payslip.fileName ??
            `Payslip ${index + 1}`;

        headerRow.appendChild(th);
    });

    tableHead.appendChild(headerRow);

    // --------------------------------
    // Collect all field names
    // --------------------------------
    const fieldNames = new Set();
    results.forEach(payslip => {
        const fields = payslip.extractionResults;
        if (fields) {
            Object.keys(fields).forEach(field => {
                fieldNames.add(field);
            });
        }
    });

    // --------------------------------
    // Create rows
    // --------------------------------
    fieldNames.forEach(fieldName => {
        const row =
            document.createElement("tr");

        // Field name
        const fieldCell = document.createElement("th");
        fieldCell.textContent = fieldName;
        row.appendChild(fieldCell);

        // Values for each payslip
        results.forEach(payslip => {
            const cell =
                document.createElement("td");
            const fields =
                payslip.extractionResults;

            if (fields && fields[fieldName] !== undefined) {
                cell.textContent =
                    fields[fieldName];
            } else {
                cell.textContent = "—";
                cell.classList.add(
                    "text-muted"
                );
            }
            row.appendChild(cell);
        });
        tableBody.appendChild(row);
    });
}

function displayResults(data) {
    const resultsSection = document.getElementById("resultsSection");
    const tableHead = document.getElementById("resultsTableHead");
    const tableBody = document.getElementById("resultsTableBody");

    // Clear previous results
    tableHead.innerHTML = "";
    tableBody.innerHTML = "";

    if (!data || data.length === 0) {
        resultsSection.classList.add("d-none");
        return;
    }

    // -------------------------------------------------
    // Create table header
    // -------------------------------------------------
    let headerRow = "<tr>";
    headerRow += "<th>Field</th>";

    data.forEach(result => {
        headerRow += `<th>${result.fileName}</th>`;
    });

    headerRow += "</tr>";
    tableHead.innerHTML = headerRow;

    // -------------------------------------------------
    // Get all field names
    // -------------------------------------------------
    const fieldNames = new Set();
    data.forEach(result => {
        if (result.fields) {
            Object.keys(result.fields).forEach(fieldName => {
                fieldNames.add(fieldName);
            });
        }
    });

    // -------------------------------------------------
    // Create table rows
    // -------------------------------------------------
    fieldNames.forEach(fieldName => {
        let row = "<tr>";
        // First column = field name
        row += `<th>${fieldName}</th>`;

        // Remaining columns = values from each payslip
        data.forEach(result => {
            const value = result.fields
                ? result.fields[fieldName]
                : null;
            row += `<td>${value ?? ""}</td>`;
        });

        row += "</tr>";
        tableBody.innerHTML += row;
    });

    // Show table
    resultsSection.classList.remove("d-none");
}

function showError(message) {
    console.log(message);
    const errorMessage = document.getElementById("errorMessage");

    errorMessage.textContent = message;
    errorMessage.classList.remove("d-none");
}

function hideError() {
    const errorMessage = document.getElementById("errorMessage");
    errorMessage.textContent = "";
    errorMessage.classList.add("d-none");
}

/*
        Save the results to file
 */
function downloadCsv() {
    if (!currentResults || currentResults.length === 0) {
        return;
    }

    const csv = createCsv(currentResults);
    const blob = new Blob(
        [csv],
        { type: "text/csv;charset=utf-8;" }
    );
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");

    link.href = url;
    link.download = "payslip-results.csv";

    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
}

function createCsv(results) {
    // Collect all possible field names
    const fieldNames = new Set();

    results.forEach(result => {
        Object.keys(result.fields || {}).forEach(fieldName => {
            fieldNames.add(fieldName);
        });
    });
    const fields = Array.from(fieldNames);

    // Header
    const headers = [
        "FileName",
        ...fields
    ];

    const rows = [];
    rows.push(headers);

    // Data rows
    results.forEach(result => {
        const row = [
            result.fileName
        ];

        fields.forEach(fieldName => {
            const value =
                result.fields?.[fieldName] ?? "";
            row.push(value);
        });
        rows.push(row);
    });
    return rows
        .map(row => row.map(escapeCsvValue).join(","))
        .join("\r\n");
}

function escapeCsvValue(value) {

    value = String(value ?? "");

    // Escape quotes
    value = value.replace(/"/g, '""');

    // Put every value inside quotes
    return `"${value}"`;
}