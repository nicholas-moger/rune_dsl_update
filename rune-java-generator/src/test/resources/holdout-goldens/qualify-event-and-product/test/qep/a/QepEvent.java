package test.qep.a;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;
import test.qep.a.meta.QepEventMeta;

import static java.util.Optional.ofNullable;

/**
 * The event root.
 * @version 0.0.0
 */
@RosettaDataType(value="QepEvent", builder=QepEvent.QepEventBuilderImpl.class, version="0.0.0")
@RuneDataType(value="QepEvent", model="test", builder=QepEvent.QepEventBuilderImpl.class, version="0.0.0")
public interface QepEvent extends RosettaModelObject {

	QepEventMeta metaData = new QepEventMeta();

	/*********************** Getter Methods  ***********************/
	String getKind();

	/*********************** Build Methods  ***********************/
	QepEvent build();
	
	QepEvent.QepEventBuilder toBuilder();
	
	static QepEvent.QepEventBuilder builder() {
		return new QepEvent.QepEventBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends QepEvent> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends QepEvent> getType() {
		return QepEvent.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("kind"), String.class, getKind(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface QepEventBuilder extends QepEvent, RosettaModelObjectBuilder {
		QepEvent.QepEventBuilder setKind(String kind);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("kind"), String.class, getKind(), this);
		}
		

		QepEvent.QepEventBuilder prune();
	}

	/*********************** Immutable Implementation of QepEvent  ***********************/
	class QepEventImpl implements QepEvent {
		private final String kind;
		
		protected QepEventImpl(QepEvent.QepEventBuilder builder) {
			this.kind = builder.getKind();
		}
		
		@Override
		@RosettaAttribute("kind")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("kind")
		public String getKind() {
			return kind;
		}
		
		@Override
		public QepEvent build() {
			return this;
		}
		
		@Override
		public QepEvent.QepEventBuilder toBuilder() {
			QepEvent.QepEventBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(QepEvent.QepEventBuilder builder) {
			ofNullable(getKind()).ifPresent(builder::setKind);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			QepEvent _that = getType().cast(o);
		
			if (!Objects.equals(kind, _that.getKind())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (kind != null ? kind.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "QepEvent {" +
				"kind=" + this.kind +
			'}';
		}
	}

	/*********************** Builder Implementation of QepEvent  ***********************/
	class QepEventBuilderImpl implements QepEvent.QepEventBuilder {
	
		protected String kind;
		
		@Override
		@RosettaAttribute("kind")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("kind")
		public String getKind() {
			return kind;
		}
		
		@RosettaAttribute("kind")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("kind")
		@Override
		public QepEvent.QepEventBuilder setKind(String _kind) {
			this.kind = _kind == null ? null : _kind;
			return this;
		}
		
		@Override
		public QepEvent build() {
			return new QepEvent.QepEventImpl(this);
		}
		
		@Override
		public QepEvent.QepEventBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public QepEvent.QepEventBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getKind()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public QepEvent.QepEventBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			QepEvent.QepEventBuilder o = (QepEvent.QepEventBuilder) other;
			
			
			merger.mergeBasic(getKind(), o.getKind(), this::setKind);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			QepEvent _that = getType().cast(o);
		
			if (!Objects.equals(kind, _that.getKind())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (kind != null ? kind.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "QepEventBuilder {" +
				"kind=" + this.kind +
			'}';
		}
	}
}
