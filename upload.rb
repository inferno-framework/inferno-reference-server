require 'httparty'
require 'pry'

FHIR_SERVER = 'http://localhost:8080/r4'

def upload_us_core_resources
  file_path = File.join(__dir__, 'us-core-r4-resources', '*.json')
  filenames = Dir.glob(file_path)
                .select { |filename| filename.end_with? '.json' }
  puts "#{filenames.length} resources to upload"
  old_retry_count = filenames.length
  loop do
    filenames_to_retry = []
    filenames.each do |filename|
      resource = JSON.parse(File.read(filename), symbolize_names: true)
      if filename.end_with? ".transaction.json"

        patient_identifier = patient_identifier_in_transaction(resource)
        record_exists = record_exists_on_server?(patient_identifier)

        if record_exists
          puts "Patient with identifier #{patient_identifier} already exists, skipping."
        else
          response = execute_transaction(resource)
          filenames_to_retry << filename unless response.success?
        end
      else
        response = upload_resource(resource)
        filenames_to_retry << filename unless response.success?
      end

    end
    break if filenames_to_retry.empty?
    retry_count = filenames_to_retry.length
    if retry_count == old_retry_count
      puts "Unable to upload #{retry_count} resources:"
      puts filenames.join("\n")
      break
    end
    puts "#{retry_count} resources to retry"
    filenames = filenames_to_retry
    old_retry_count = retry_count
  end
end

def upload_resource(resource)
  resource_type = resource[:resourceType]
  id = resource[:id]
  HTTParty.put(
    "#{FHIR_SERVER}/#{resource_type}/#{id}",
    body: resource.to_json,
    headers: { 'Content-Type': 'application/json' }
  )
end

def patient_identifier_in_transaction(transaction)

  patient_record = transaction[:entry]&.find {|r| r[:resource][:resourceType] == 'Patient'}
  identifier = patient_record[:resource][:identifier].first
  "#{identifier[:system]}|#{identifier[:value]}"
end

def record_exists_on_server?(patient_identifier)

  response = HTTParty.get(
    "#{FHIR_SERVER}/Patient",
    query: { identifier: patient_identifier },
    headers: { 'Content-Type': 'application/json' }
  )
  JSON.parse(response.body)['entry']&.any?

end

def execute_transaction(transaction)

  HTTParty.post(
    FHIR_SERVER,
    body: transaction.to_json,
    headers: { 'Content-Type': 'application/json' }
  )
end

upload_us_core_resources
